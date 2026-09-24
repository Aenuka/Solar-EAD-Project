using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Endpoints;

public static class DashboardEndpoints
{
    public static void MapDashboardEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/v1/dashboard")
            .WithTags("Dashboard")
            .RequireAuthorization(policy => policy.RequireRole(Roles.Backoffice));

        // GET /api/v1/dashboard
        group.MapGet("", (DashboardService service, CancellationToken ct) => service.GetAsync(ct));
    }
}
