/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Clears unauthorized web sessions, redirects users to login, and renders API failure pages.
 */

using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using SolarMicrogrid.Web.Presentation;
using SolarMicrogrid.Web.ViewModels;

namespace SolarMicrogrid.Web.ApiClients;

public sealed class ApiExceptionFilter : IAsyncExceptionFilter
{
    public async Task OnExceptionAsync(ExceptionContext context)
    {
        if (context.Exception is not ApiFailureException failure)
        {
            return;
        }

        if (failure.StatusCode == 401)
        {
            await context.HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            context.Result = new RedirectToActionResult("Login", "Account", new { expired = true });
        }
        else
        {
            var model = new ErrorViewModel(failure.StatusCode, failure.Message);
            context.Result = new ReactPageResult
            {
                Page = "Error",
                StatusCode = failure.StatusCode,
                Model = model
            };
        }

        context.ExceptionHandled = true;
    }
}
