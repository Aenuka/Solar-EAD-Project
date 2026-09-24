using System.Security.Claims;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Endpoints;

// Reference: Julio Casal YouTube tutorials
// https://www.youtube.com/@juliocasal
public static class AuthEndpoints
{
    public static void MapAuthEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/v1/auth").WithTags("Auth");

        // POST /api/v1/auth/staff/login
        group.MapPost("/staff/login", (StaffLoginRequest request, AuthService service, CancellationToken ct) =>
            service.LoginStaffAsync(request, ct))
            .WithJsonBody<StaffLoginRequest>()
            .AllowAnonymous()
            .RequireRateLimiting("auth");

        // POST /api/v1/auth/prosumers/login
        group.MapPost("/prosumers/login", (ProsumerLoginRequest request, AuthService service, CancellationToken ct) =>
            service.LoginProsumerAsync(request, ct))
            .WithJsonBody<ProsumerLoginRequest>()
            .AllowAnonymous()
            .RequireRateLimiting("auth");

        // GET /api/v1/auth/me
        group.MapGet("/me", async (ClaimsPrincipal user, AuthService service, CancellationToken ct) =>
        {
            var account = await service.FindAccountAsync(user.FindFirstValue("sub")!, user.FindFirstValue("role")!, ct)
                ?? throw ApiException.InvalidCredentials();

            return new CurrentUserResponse(account.Id, account.FullName, account.Role);
        });

        // POST /api/v1/auth/logout
        group.MapPost("/logout", async (ClaimsPrincipal user, AuthService service, CancellationToken ct) =>
        {
            await service.LogoutAsync(user.FindFirstValue("sub")!, user.FindFirstValue("role")!, ct);
            return Results.NoContent();
        });
    }
}
