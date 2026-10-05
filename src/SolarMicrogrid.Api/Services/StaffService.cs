/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Creates and updates staff accounts, roles, and statuses while protecting privileged accounts and revoking changed sessions.
 */

using SolarMicrogrid.Api.Data;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Api.Security;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public sealed class StaffService(StaffRepository repository, PasswordService passwords)
{
    public async Task<PageResponse<StaffResponse>> ListAsync(int page, int pageSize, CancellationToken ct)
    {
        var (users, total) = await repository.ListAsync(page, pageSize, ct);
        var items = users.Select(user => user.ToResponse()).ToList();
        return new PageResponse<StaffResponse>(items, total, page, pageSize);
    }

    public async Task<StaffResponse> GetAsync(string id, CancellationToken ct)
    {
        var user = await repository.FindByIdAsync(id, ct) ?? throw ApiException.NotFound();
        return user.ToResponse();
    }

    public async Task<StaffResponse> CreateAsync(CreateStaffRequest request, string actorId, CancellationToken ct)
    {
        if (request.Role is null || !Enum.IsDefined(request.Role.Value))
        {
            throw ApiException.Invalid("Choose a valid staff role.");
        }

        StaffUser user = new()
        {
            Id = Guid.NewGuid().ToString("N"),
            Username = request.Username.Trim().ToLowerInvariant(),
            FullName = request.FullName.Trim(),
            Email = request.Email.Trim(),
            Role = request.Role.Value.ToString()
        };
        user.PasswordHash = passwords.Hash(user, request.Password);
        user.RecentEvents = user.WithEvent("StaffCreated", actorId);

        try
        {
            await repository.InsertAsync(user, ct);
        }
        catch (DuplicateAccountException)
        {
            throw ApiException.Conflict("This username is already registered.");
        }

        return user.ToResponse();
    }

    public async Task<StaffResponse> UpdateAsync(string id, UpdateStaffRequest request, string actorId, CancellationToken ct)
    {
        var user = await repository.FindByIdAsync(id, ct) ?? throw ApiException.NotFound();
        ResponseMapping.CheckVersion(user.Version, request.Version);

        if (request.Role is null || request.Status is null ||
            !Enum.IsDefined(request.Role.Value) || !Enum.IsDefined(request.Status.Value))
        {
            throw ApiException.Invalid("Choose a valid role and account status.");
        }

        var role = request.Role.Value.ToString();
        var securityChanged = role != user.Role || request.Status != user.Status;
        if ((user.IsProtected || id == actorId) && securityChanged)
        {
            throw ApiException.Forbidden("You cannot change your own role/status or the protected bootstrap account's role/status.");
        }

        var previousVersion = user.Version;
        user.FullName = request.FullName.Trim();
        user.Email = request.Email.Trim();
        user.Role = role;
        user.Status = request.Status.Value;
        user.Version++;
        user.UpdatedAt = DateTime.UtcNow;
        user.RecentEvents = user.WithEvent("StaffUpdated", actorId);

        if (securityChanged)
        {
            user.SecurityVersion++;
        }

        if (!await repository.ReplaceAsync(user, previousVersion, ct))
        {
            throw ApiException.Conflict("This account changed. Reload it before trying again.");
        }

        return user.ToResponse();
    }
}
