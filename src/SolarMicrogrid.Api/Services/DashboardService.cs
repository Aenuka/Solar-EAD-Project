using SolarMicrogrid.Api.Repositories;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public sealed class DashboardService(IProsumerRepository prosumers, IStaffRepository staff)
{
    public async Task<DashboardResponse> GetAsync(CancellationToken ct) => new(
        await prosumers.CountAsync(AccountStatus.Active, null, ct),
        await prosumers.CountAsync(AccountStatus.Inactive, null, ct),
        await prosumers.CountAsync(null, RequestStatus.Pending, ct),
        (await staff.ListAsync(1, 1, ct)).Total);
}
