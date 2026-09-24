using System.Security.Claims;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;
using SolarMicrogrid.Web.ViewModels;

namespace SolarMicrogrid.Web.Controllers;

public sealed class AccountController(MicrogridApiClient api) : Controller
{
    [AllowAnonymous, HttpGet]
    public IActionResult Login(bool expired = false)
    {
        if (User.Identity?.IsAuthenticated == true)
        {
            return RedirectToAction("Index", "Home");
        }

        ViewBag.Expired = expired;
        return View(new LoginViewModel());
    }

    [AllowAnonymous, HttpPost]
    public async Task<IActionResult> Login(LoginViewModel model, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        try
        {
            var request = new StaffLoginRequest(model.Username, model.Password);
            var session = await api.PostAsync<AuthResponse>("auth/staff/login", request, ct);
            var claims = new[]
            {
                new Claim(ClaimTypes.NameIdentifier, session.Id),
                new Claim(ClaimTypes.Name, session.FullName),
                new Claim(ClaimTypes.Role, session.Role)
            };
            var identity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);
            var properties = new AuthenticationProperties
            {
                IsPersistent = false,
                ExpiresUtc = session.ExpiresAt,
                AllowRefresh = false
            };
            properties.StoreTokens([new AuthenticationToken { Name = "access_token", Value = session.AccessToken }]);

            await HttpContext.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme,
                new ClaimsPrincipal(identity), properties);
            return RedirectToAction("Index", "Home");
        }
        catch (ApiFailureException exception)
        {
            ModelState.AddModelError("", exception.Message);
            return View(model);
        }
    }

    [HttpPost]
    public async Task<IActionResult> Logout(CancellationToken ct)
    {
        try
        {
            await api.LogoutAsync(ct);
        }
        catch (ApiFailureException exception) when (exception.StatusCode == 401)
        {
            // The API session has already been revoked; still clear the browser cookie.
        }
        catch (ApiFailureException)
        {
            TempData["LogoutNotice"] = "Signed out on this browser. Account services were unavailable, so sessions on other devices may remain active until expiry.";
        }

        await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
        return RedirectToAction(nameof(Login));
    }

    [AllowAnonymous, HttpGet]
    public IActionResult AccessDenied()
    {
        Response.StatusCode = 403;
        return View("~/Views/Shared/Error.cshtml", new ErrorViewModel(403, "Your role does not have access to this page."));
    }
}
