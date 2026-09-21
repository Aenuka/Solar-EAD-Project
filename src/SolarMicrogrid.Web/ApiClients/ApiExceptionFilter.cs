using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.AspNetCore.Mvc.ViewFeatures;
using SolarMicrogrid.Web.ViewModels;

namespace SolarMicrogrid.Web.ApiClients;

public sealed class ApiExceptionFilter : IAsyncExceptionFilter
{
    public async Task OnExceptionAsync(ExceptionContext context)
    {
        if (context.Exception is not ApiFailureException failure) return;
        if (failure.StatusCode == 401)
        {
            await context.HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            context.Result = new RedirectToActionResult("Login", "Account", new { expired = true });
        }
        else
        {
            context.Result = new ViewResult
            {
                ViewName = "~/Views/Shared/Error.cshtml", StatusCode = failure.StatusCode,
                ViewData = new ViewDataDictionary(new Microsoft.AspNetCore.Mvc.ModelBinding.EmptyModelMetadataProvider(), context.ModelState)
                { Model = new ErrorViewModel(failure.StatusCode, failure.Message) }
            };
        }
        context.ExceptionHandled = true;
    }
}
