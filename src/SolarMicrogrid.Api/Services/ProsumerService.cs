using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Api.Repositories;
using SolarMicrogrid.Api.Security;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public sealed class ProsumerService(IProsumerRepository repository, PasswordService passwords)
{
    public async Task<ProsumerResponse> RegisterAsync(RegisterProsumerRequest request, CancellationToken ct)
    {
        var user = new Prosumer
        {
            Id = NationalIdentity.Normalize(request.Nic), Role = Roles.Prosumer, FullName = request.FullName.Trim(),
            Email = request.Email.Trim(), Phone = request.Phone.Trim(), Address = request.Address.Trim()
        };
        user = user with { PasswordHash = passwords.Hash(user, request.Password), RecentEvents = user.WithEvent("Registered", user.Id) };
        try { await repository.InsertAsync(user, ct); }
        catch (DuplicateAccountException) { throw ApiException.Conflict("An account is already registered with this NIC."); }
        return user.ToResponse();
    }

    public async Task<ProsumerResponse> GetAsync(string nic, CancellationToken ct) => (await FindAsync(nic, ct)).ToResponse();

    public async Task<PageResponse<ProsumerResponse>> ListAsync(int page, int size, AccountStatus? status,
        RequestStatus? requestStatus, string? search, CancellationToken ct)
    {
        var (items, total) = await repository.ListAsync(page, size, status, requestStatus, search, ct);
        return new(items.Select(x => x.ToResponse()).ToList(), total, page, size);
    }

    public async Task<ProsumerResponse> UpdateProfileAsync(string nic, UpdateProfileRequest request, string actorId, CancellationToken ct)
    {
        var user = await FindAsync(nic, ct);
        ResponseMapping.CheckVersion(user.Version, request.Version);
        var updated = user with
        {
            FullName = request.FullName.Trim(), Email = request.Email.Trim(), Phone = request.Phone.Trim(), Address = request.Address.Trim(),
            Version = user.Version + 1, UpdatedAt = DateTime.UtcNow, RecentEvents = user.WithEvent("ProfileUpdated", actorId)
        };
        return await SaveAsync(user, updated, ct);
    }

    public async Task<ProsumerResponse> RequestDeactivationAsync(string nic, DeactivationRequestInput request, CancellationToken ct)
    {
        var user = await FindAsync(nic, ct);
        ResponseMapping.CheckVersion(user.Version, request.Version);
        if (user.Status != AccountStatus.Active) throw ApiException.Conflict("Only active accounts can request deactivation.");
        if (user.DeactivationRequest?.Status == RequestStatus.Pending) throw ApiException.Conflict("A deactivation request is already pending.");
        var updated = user with
        {
            DeactivationRequest = new() { Reason = request.Reason.Trim() }, Version = user.Version + 1,
            UpdatedAt = DateTime.UtcNow, RecentEvents = user.WithEvent("DeactivationRequested", user.Id, request.Reason.Trim())
        };
        return await SaveAsync(user, updated, ct);
    }

    public async Task<ProsumerResponse> DecideAsync(string nic, string requestId, DecisionRequest request, string actorId, CancellationToken ct)
    {
        var user = await FindAsync(nic, ct);
        ResponseMapping.CheckVersion(user.Version, request.Version);
        if (request.Decision is null || !Enum.IsDefined(request.Decision.Value)) throw ApiException.Invalid("Choose Approved or Rejected.");
        if (user.DeactivationRequest?.Id != requestId) throw ApiException.NotFound();
        if (user.Status != AccountStatus.Active || user.DeactivationRequest.Status != RequestStatus.Pending)
            throw ApiException.Conflict("This request can no longer be reviewed.");
        var approved = request.Decision == Decision.Approved;
        var updated = user with
        {
            Status = approved ? AccountStatus.Inactive : AccountStatus.Active,
            SecurityVersion = user.SecurityVersion + (approved ? 1 : 0),
            Version = user.Version + 1, UpdatedAt = DateTime.UtcNow,
            DeactivationRequest = user.DeactivationRequest with
            {
                Status = approved ? RequestStatus.Approved : RequestStatus.Rejected,
                ReviewedAt = DateTime.UtcNow, ReviewedBy = actorId, DecisionNote = request.Note.Trim()
            },
            RecentEvents = user.WithEvent(approved ? "DeactivationApproved" : "DeactivationRejected", actorId, request.Note.Trim())
        };
        // Request decision, account status, revocation, and recent audit entry are one atomic document replacement.
        return await SaveAsync(user, updated, ct);
    }

    public async Task<ProsumerResponse> ReactivateAsync(string nic, ReactivateRequest request, string actorId, CancellationToken ct)
    {
        var user = await FindAsync(nic, ct);
        ResponseMapping.CheckVersion(user.Version, request.Version);
        if (user.Status != AccountStatus.Inactive) throw ApiException.Conflict("Only inactive accounts can be reactivated.");
        var updated = user with
        {
            Status = AccountStatus.Active, SecurityVersion = user.SecurityVersion + 1,
            Version = user.Version + 1, UpdatedAt = DateTime.UtcNow,
            RecentEvents = user.WithEvent("Reactivated", actorId, request.Note.Trim())
        };
        return await SaveAsync(user, updated, ct);
    }

    private async Task<Prosumer> FindAsync(string nic, CancellationToken ct) =>
        await repository.FindAsync(NationalIdentity.Normalize(nic), ct) ?? throw ApiException.NotFound();

    private async Task<ProsumerResponse> SaveAsync(Prosumer previous, Prosumer updated, CancellationToken ct)
    {
        if (!await repository.ReplaceAsync(updated, previous.Version, ct))
            throw ApiException.Conflict("This account changed. Reload it before trying again.");
        return updated.ToResponse();
    }
}
