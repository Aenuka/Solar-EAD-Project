using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Controllers;

[ApiController, Authorize(Roles = Roles.Backoffice), Route("api/v1/dashboard")]
public sealed class DashboardController(DashboardService service) : ControllerBase
{
    [HttpGet] public Task<DashboardResponse> Get(CancellationToken ct) => service.GetAsync(ct);
}
