using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Api.Repositories;
using SolarMicrogrid.Api.Security;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public sealed class StaffService(IStaffRepository repository, PasswordService passwords)
{
    public async Task<PageResponse<StaffResponse>> ListAsync(int page, int size, CancellationToken ct)
    {
        var (items, total) = await repository.ListAsync(page, size, ct);
        return new(items.Select(x => x.ToResponse()).ToList(), total, page, size);
    }

    public async Task<StaffResponse> GetAsync(string id, CancellationToken ct) =>
        (await repository.FindByIdAsync(id, ct) ?? throw ApiException.NotFound()).ToResponse();

    public async Task<StaffResponse> CreateAsync(CreateStaffRequest request, string actorId, CancellationToken ct)
    {
        if (request.Role is null || !Enum.IsDefined(request.Role.Value)) throw ApiException.Invalid("Choose a valid staff role.");
        var user = new StaffUser
        {
            Id = Guid.NewGuid().ToString("N"), Username = request.Username.Trim().ToLowerInvariant(),
            FullName = request.FullName.Trim(), Email = request.Email.Trim(), Role = request.Role.Value.ToString()
        };
        user = user with { PasswordHash = passwords.Hash(user, request.Password), RecentEvents = user.WithEvent("StaffCreated", actorId) };
        try { await repository.InsertAsync(user, ct); }
        catch (DuplicateAccountException) { throw ApiException.Conflict("This username is already registered."); }
        return user.ToResponse();
    }

    public async Task<StaffResponse> UpdateAsync(string id, UpdateStaffRequest request, string actorId, CancellationToken ct)
    {
        var user = await repository.FindByIdAsync(id, ct) ?? throw ApiException.NotFound();
        ResponseMapping.CheckVersion(user.Version, request.Version);
        if (request.Role is null || request.Status is null || !Enum.IsDefined(request.Role.Value) || !Enum.IsDefined(request.Status.Value))
            throw ApiException.Invalid("Choose a valid role and account status.");
        var role = request.Role.Value.ToString();
        var securityChanged = role != user.Role || request.Status != user.Status;
        if ((user.IsProtected || id == actorId) && securityChanged)
            throw ApiException.Forbidden("You cannot change your own role/status or the protected bootstrap account's role/status.");
        var updated = user with
        {
            FullName = request.FullName.Trim(), Email = request.Email.Trim(), Role = role, Status = request.Status.Value,
            Version = user.Version + 1, SecurityVersion = user.SecurityVersion + (securityChanged ? 1 : 0),
            UpdatedAt = DateTime.UtcNow, RecentEvents = user.WithEvent("StaffUpdated", actorId)
        };
        if (!await repository.ReplaceAsync(updated, user.Version, ct))
            throw ApiException.Conflict("This account changed. Reload it before trying again.");
        return updated.ToResponse();
    }
}
