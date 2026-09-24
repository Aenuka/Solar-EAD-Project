using SolarMicrogrid.Api.Data;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public sealed class DashboardService(ProsumerRepository prosumers, StaffRepository staff)
{
    public async Task<DashboardResponse> GetAsync(CancellationToken ct)
    {
        var activeProsumers = await prosumers.CountAsync(AccountStatus.Active, null, ct);
        var inactiveProsumers = await prosumers.CountAsync(AccountStatus.Inactive, null, ct);
        var pendingRequests = await prosumers.CountAsync(null, RequestStatus.Pending, ct);
        var staffPage = await staff.ListAsync(1, 1, ct);

        return new DashboardResponse(activeProsumers, inactiveProsumers, pendingRequests, staffPage.Total);
    }
}
