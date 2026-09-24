using SolarMicrogrid.Api.Data;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Api.Security;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public sealed class ProsumerService(ProsumerRepository repository, PasswordService passwords)
{
    public async Task<ProsumerResponse> RegisterAsync(RegisterProsumerRequest request, CancellationToken ct)
    {
        Prosumer user = new()
        {
            Id = NationalIdentity.Normalize(request.Nic),
            Role = Roles.Prosumer,
            FullName = request.FullName.Trim(),
            Email = request.Email.Trim(),
            Phone = request.Phone.Trim(),
            Address = request.Address.Trim()
        };
        user.PasswordHash = passwords.Hash(user, request.Password);
        user.RecentEvents = user.WithEvent("Registered", user.Id);

        try
        {
            await repository.InsertAsync(user, ct);
        }
        catch (DuplicateAccountException)
        {
            throw ApiException.Conflict("An account is already registered with this NIC.");
        }

        return user.ToResponse();
    }

    public async Task<ProsumerResponse> GetAsync(string nic, CancellationToken ct)
    {
        var user = await FindAsync(nic, ct);
        return user.ToResponse();
    }

    public async Task<PageResponse<ProsumerResponse>> ListAsync(int page, int pageSize, AccountStatus? status,
        RequestStatus? requestStatus, string? search, CancellationToken ct)
    {
        var (users, total) = await repository.ListAsync(page, pageSize, status, requestStatus, search, ct);
        var items = users.Select(user => user.ToResponse()).ToList();
        return new PageResponse<ProsumerResponse>(items, total, page, pageSize);
    }

    public async Task<ProsumerResponse> UpdateProfileAsync(string nic, UpdateProfileRequest request, string actorId, CancellationToken ct)
    {
        var user = await FindAsync(nic, ct);
        ResponseMapping.CheckVersion(user.Version, request.Version);

        user.FullName = request.FullName.Trim();
        user.Email = request.Email.Trim();
        user.Phone = request.Phone.Trim();
        user.Address = request.Address.Trim();
        user.RecentEvents = user.WithEvent("ProfileUpdated", actorId);

        return await SaveAsync(user, ct);
    }

    public async Task<ProsumerResponse> RequestDeactivationAsync(string nic, DeactivationRequestInput request, CancellationToken ct)
    {
        var user = await FindAsync(nic, ct);
        ResponseMapping.CheckVersion(user.Version, request.Version);

        if (user.Status != AccountStatus.Active)
        {
            throw ApiException.Conflict("Only active accounts can request deactivation.");
        }
        if (user.DeactivationRequest?.Status == RequestStatus.Pending)
        {
            throw ApiException.Conflict("A deactivation request is already pending.");
        }

        user.DeactivationRequest = new DeactivationRequest { Reason = request.Reason.Trim() };
        user.RecentEvents = user.WithEvent("DeactivationRequested", user.Id, request.Reason.Trim());

        return await SaveAsync(user, ct);
    }

    public async Task<ProsumerResponse> DecideAsync(string nic, string requestId, DecisionRequest request, string actorId, CancellationToken ct)
    {
        var user = await FindAsync(nic, ct);
        ResponseMapping.CheckVersion(user.Version, request.Version);

        if (request.Decision is null || !Enum.IsDefined(request.Decision.Value))
        {
            throw ApiException.Invalid("Choose Approved or Rejected.");
        }
        if (user.DeactivationRequest?.Id != requestId)
        {
            throw ApiException.NotFound();
        }
        if (user.Status != AccountStatus.Active || user.DeactivationRequest.Status != RequestStatus.Pending)
        {
            throw ApiException.Conflict("This request can no longer be reviewed.");
        }

        var approved = request.Decision == Decision.Approved;
        user.Status = approved ? AccountStatus.Inactive : AccountStatus.Active;
        if (approved)
        {
            user.SecurityVersion++;
        }

        user.DeactivationRequest.Status = approved ? RequestStatus.Approved : RequestStatus.Rejected;
        user.DeactivationRequest.ReviewedAt = DateTime.UtcNow;
        user.DeactivationRequest.ReviewedBy = actorId;
        user.DeactivationRequest.DecisionNote = request.Note.Trim();
        user.RecentEvents = user.WithEvent(approved ? "DeactivationApproved" : "DeactivationRejected", actorId, request.Note.Trim());

        // Save the decision, account status and session revocation in one MongoDB write.
        return await SaveAsync(user, ct);
    }

    public async Task<ProsumerResponse> ReactivateAsync(string nic, ReactivateRequest request, string actorId, CancellationToken ct)
    {
        var user = await FindAsync(nic, ct);
        ResponseMapping.CheckVersion(user.Version, request.Version);

        if (user.Status != AccountStatus.Inactive)
        {
            throw ApiException.Conflict("Only inactive accounts can be reactivated.");
        }

        user.Status = AccountStatus.Active;
        user.SecurityVersion++;
        user.RecentEvents = user.WithEvent("Reactivated", actorId, request.Note.Trim());

        return await SaveAsync(user, ct);
    }

    private async Task<Prosumer> FindAsync(string nic, CancellationToken ct) =>
        await repository.FindAsync(NationalIdentity.Normalize(nic), ct) ?? throw ApiException.NotFound();

    private async Task<ProsumerResponse> SaveAsync(Prosumer user, CancellationToken ct)
    {
        var previousVersion = user.Version;
        user.Version++;
        user.UpdatedAt = DateTime.UtcNow;

        if (!await repository.ReplaceAsync(user, previousVersion, ct))
        {
            throw ApiException.Conflict("This account changed. Reload it before trying again.");
        }

        return user.ToResponse();
    }
}
