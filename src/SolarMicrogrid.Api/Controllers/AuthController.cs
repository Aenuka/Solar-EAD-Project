using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Controllers;

[ApiController, Route("api/v1/auth")]
public sealed class AuthController(AuthService service) : ControllerBase
{
    [AllowAnonymous, EnableRateLimiting("auth"), HttpPost("staff/login")]
    public Task<AuthResponse> StaffLogin(StaffLoginRequest request, CancellationToken ct) => service.LoginStaffAsync(request, ct);

    [AllowAnonymous, EnableRateLimiting("auth"), HttpPost("prosumers/login")]
    public Task<AuthResponse> ProsumerLogin(ProsumerLoginRequest request, CancellationToken ct) => service.LoginProsumerAsync(request, ct);

    [HttpGet("me")]
    public async Task<CurrentUserResponse> Me(CancellationToken ct)
    {
        var user = await service.FindAccountAsync(User.FindFirstValue("sub")!, User.FindFirstValue("role")!, ct)
            ?? throw ApiException.InvalidCredentials();
        return new(user.Id, user.FullName, user.Role);
    }

    [HttpPost("logout")]
    public async Task<IActionResult> Logout(CancellationToken ct)
    {
        await service.LogoutAsync(User.FindFirstValue("sub")!, User.FindFirstValue("role")!, ct);
        return NoContent();
    }
}
