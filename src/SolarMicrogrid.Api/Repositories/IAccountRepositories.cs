using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Repositories;

public interface IStaffRepository
{
    Task<StaffUser?> FindByIdAsync(string id, CancellationToken ct);
    Task<StaffUser?> FindByUsernameAsync(string username, CancellationToken ct);
    Task<(List<StaffUser> Items, long Total)> ListAsync(int page, int size, CancellationToken ct);
    Task InsertAsync(StaffUser user, CancellationToken ct);
    Task<bool> ReplaceAsync(StaffUser user, long expectedVersion, CancellationToken ct);
    Task RevokeSessionsAsync(string id, CancellationToken ct);
}

public interface IProsumerRepository
{
    Task<Prosumer?> FindAsync(string nic, CancellationToken ct);
    Task<(List<Prosumer> Items, long Total)> ListAsync(int page, int size, AccountStatus? status,
        RequestStatus? requestStatus, string? search, CancellationToken ct);
    Task<long> CountAsync(AccountStatus? status, RequestStatus? requestStatus, CancellationToken ct);
    Task InsertAsync(Prosumer prosumer, CancellationToken ct);
    Task<bool> ReplaceAsync(Prosumer prosumer, long expectedVersion, CancellationToken ct);
    Task RevokeSessionsAsync(string nic, CancellationToken ct);
}

public sealed class DuplicateAccountException : Exception;
