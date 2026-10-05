/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Configures JWT bearer validation, role claims, account session checks, and login rate limiting.
 */

using System.Text;
using System.Threading.RateLimiting;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using SolarMicrogrid.Api.Configuration;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Security;

public static class AuthenticationExtensions
{
    public static void AddApiAuthentication(this WebApplicationBuilder builder)
    {
        builder.Services.AddOptions<JwtOptions>()
            .BindConfiguration("Jwt")
            .ValidateDataAnnotations()
            .ValidateOnStart();

        // Reference: Julio Casal YouTube tutorials
        // https://www.youtube.com/@juliocasal
        builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme).AddJwtBearer();
        builder.Services.AddAuthorization(options =>
        {
            options.FallbackPolicy = new AuthorizationPolicyBuilder()
                .RequireAuthenticatedUser()
                .Build();
        });

        builder.Services.AddOptions<JwtBearerOptions>(JwtBearerDefaults.AuthenticationScheme)
            .Configure<IOptions<JwtOptions>>((options, settings) =>
            {
                var jwt = settings.Value;
                options.MapInboundClaims = false;
                options.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuerSigningKey = true,
                    IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwt.SigningKey)),
                    ValidateIssuer = true,
                    ValidIssuer = jwt.Issuer,
                    ValidateAudience = true,
                    ValidAudience = jwt.Audience,
                    ValidateLifetime = true,
                    RequireExpirationTime = true,
                    ClockSkew = TimeSpan.FromSeconds(15),
                    ValidAlgorithms = [SecurityAlgorithms.HmacSha256],
                    NameClaimType = "name",
                    RoleClaimType = "role"
                };
                options.Events = new JwtBearerEvents { OnTokenValidated = ValidateSessionAsync };
            });

        builder.Services.AddRateLimiter(options =>
        {
            options.RejectionStatusCode = 429;
            options.AddPolicy("auth", context => RateLimitPartition.GetFixedWindowLimiter(
                context.Connection.RemoteIpAddress?.ToString() ?? "unknown",
                _ => new FixedWindowRateLimiterOptions
                {
                    PermitLimit = builder.Configuration.GetValue("RateLimiting:AuthPermitLimit", 30),
                    Window = TimeSpan.FromMinutes(1),
                    QueueLimit = 0
                }));
            options.OnRejected = async (context, ct) =>
            {
                context.HttpContext.Response.Headers.RetryAfter = "60";
                await Results.Problem(statusCode: 429, title: "Too many attempts", detail: "Please wait a minute before trying again.")
                    .ExecuteAsync(context.HttpContext);
            };
        });
    }

    private static async Task ValidateSessionAsync(TokenValidatedContext context)
    {
        var principal = context.Principal!;
        var id = principal.FindFirst("sub")?.Value;
        var role = principal.FindFirst("role")?.Value;
        var version = principal.FindFirst("sv")?.Value;

        if (id is null || role is not (Roles.Backoffice or Roles.GridOperator or Roles.Prosumer) ||
            !long.TryParse(version, out var securityVersion))
        {
            context.Fail("Invalid session.");
            return;
        }

        // A signed token can still belong to an account that logged out or became inactive.
        var service = context.HttpContext.RequestServices.GetRequiredService<AuthService>();
        var account = await service.FindAccountAsync(id, role, context.HttpContext.RequestAborted);
        if (account is null || account.Status != AccountStatus.Active ||
            account.Role != role || account.SecurityVersion != securityVersion)
        {
            context.Fail("Session expired or revoked.");
        }
    }
}
