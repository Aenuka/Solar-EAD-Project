using System.Net;
using System.Net.Http.Headers;
using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authorization;
using SolarMicrogrid.Web.ApiClients;

namespace SolarMicrogrid.Web.Security;

public static class AuthenticationExtensions
{
    public static void AddPortalAuthentication(this WebApplicationBuilder builder)
    {
        builder.Services.AddAuthentication(CookieAuthenticationDefaults.AuthenticationScheme)
            .AddCookie(options =>
            {
                options.Cookie.Name = "SolarMicrogrid.Session";
                options.Cookie.HttpOnly = true;
                options.Cookie.SameSite = SameSiteMode.Lax;
                options.Cookie.SecurePolicy = builder.Environment.IsDevelopment()
                    ? CookieSecurePolicy.SameAsRequest
                    : CookieSecurePolicy.Always;
                options.LoginPath = "/Account/Login";
                options.AccessDeniedPath = "/Account/AccessDenied";
                options.SlidingExpiration = false;
                options.Events.OnValidatePrincipal = ValidateSessionAsync;
            });

        builder.Services.AddAuthorization(options =>
        {
            options.FallbackPolicy = new AuthorizationPolicyBuilder()
                .RequireAuthenticatedUser()
                .Build();
        });
    }

    private static async Task ValidateSessionAsync(CookieValidatePrincipalContext context)
    {
        // These pages must work even when the API is unavailable.
        if (context.Request.Path.StartsWithSegments("/Home/Error") ||
            context.Request.Path.StartsWithSegments("/Account/Logout"))
        {
            return;
        }

        var token = context.Properties.GetTokenValue("access_token");
        using var request = new HttpRequestMessage(HttpMethod.Get, "auth/me");
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);

        try
        {
            var factory = context.HttpContext.RequestServices.GetRequiredService<IHttpClientFactory>();
            var client = factory.CreateClient("session-validation");
            using var response = await client.SendAsync(request, context.HttpContext.RequestAborted);

            if (response.StatusCode is HttpStatusCode.Unauthorized or HttpStatusCode.Forbidden)
            {
                context.RejectPrincipal();
                await context.HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
            }
            else if (!response.IsSuccessStatusCode)
            {
                throw new ApiFailureException(503, "The account service is temporarily unavailable.");
            }
        }
        catch (HttpRequestException)
        {
            throw new ApiFailureException(503, "The account service is temporarily unavailable.");
        }
        catch (TaskCanceledException) when (!context.HttpContext.RequestAborted.IsCancellationRequested)
        {
            throw new ApiFailureException(503, "The account service is temporarily unavailable.");
        }
    }
}
