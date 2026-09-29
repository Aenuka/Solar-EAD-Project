using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Web.Presentation;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;
using SolarMicrogrid.Web.ViewModels;

namespace SolarMicrogrid.Web.Controllers;

[Authorize(Roles = Roles.Staff)]
public sealed class HomeController(MicrogridApiClient api) : PortalController
{
    public async Task<IActionResult> Index(CancellationToken ct)
    {
        if (User.IsInRole(Roles.GridOperator))
        {
            return ReactPage(page: "Operator");
        }
        var dashboard = await api.GetAsync<DashboardResponse>("dashboard", ct);
        return ReactPage(dashboard);
    }

    [AllowAnonymous]
    public IActionResult Error()
    {
        var failure = HttpContext.Features.Get<IExceptionHandlerFeature>()?.Error as ApiFailureException;
        var status = failure?.StatusCode ?? 500;
        Response.StatusCode = status;
        return ReactPage(new ErrorViewModel(status,
            failure?.Message ?? "Something went wrong. Please try again."), page: "Error");
    }
}
