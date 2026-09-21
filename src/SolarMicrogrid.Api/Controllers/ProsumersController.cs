using System.ComponentModel.DataAnnotations;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Controllers;

[ApiController, Route("api/v1/prosumers")]
public sealed class ProsumersController(ProsumerService service) : ControllerBase
{
    private string ActorId => User.FindFirstValue("sub")!;

    [AllowAnonymous, EnableRateLimiting("auth"), HttpPost, ProducesResponseType<ProsumerResponse>(201)]
    public async Task<ActionResult<ProsumerResponse>> Register(RegisterProsumerRequest request, CancellationToken ct)
    {
        var result = await service.RegisterAsync(request, ct);
        return CreatedAtAction(nameof(GetMine), result);
    }

    [Authorize(Roles = Roles.Prosumer), HttpGet("me")]
    public Task<ProsumerResponse> GetMine(CancellationToken ct) => service.GetAsync(ActorId, ct);

    [Authorize(Roles = Roles.Prosumer), HttpPatch("me")]
    public Task<ProsumerResponse> UpdateMine(UpdateProfileRequest request, CancellationToken ct) =>
        service.UpdateProfileAsync(ActorId, request, ActorId, ct);

    [Authorize(Roles = Roles.Prosumer), HttpPost("me/deactivation-requests"), ProducesResponseType<ProsumerResponse>(201)]
    public async Task<ActionResult<ProsumerResponse>> RequestDeactivation(DeactivationRequestInput request, CancellationToken ct)
    {
        var result = await service.RequestDeactivationAsync(ActorId, request, ct);
        return CreatedAtAction(nameof(GetMine), result);
    }

    [Authorize(Roles = Roles.Backoffice), HttpGet]
    public Task<PageResponse<ProsumerResponse>> List([Range(1, 100000)] int page = 1, [Range(1, 100)] int pageSize = 20,
        AccountStatus? status = null, RequestStatus? requestStatus = null, [StringLength(100)] string? search = null,
        CancellationToken ct = default) => service.ListAsync(page, pageSize, status, requestStatus, search, ct);

    [Authorize(Roles = Roles.Backoffice), HttpGet("{nic}")]
    public Task<ProsumerResponse> Get(string nic, CancellationToken ct) => service.GetAsync(nic, ct);

    [Authorize(Roles = Roles.Backoffice), HttpPatch("{nic}")]
    public Task<ProsumerResponse> Update(string nic, UpdateProfileRequest request, CancellationToken ct) =>
        service.UpdateProfileAsync(nic, request, ActorId, ct);

    [Authorize(Roles = Roles.Backoffice), HttpPost("{nic}/deactivation-requests/{requestId}/decision")]
    public Task<ProsumerResponse> Decide(string nic, string requestId, DecisionRequest request, CancellationToken ct) =>
        service.DecideAsync(nic, requestId, request, ActorId, ct);

    [Authorize(Roles = Roles.Backoffice), HttpPost("{nic}/reactivation")]
    public Task<ProsumerResponse> Reactivate(string nic, ReactivateRequest request, CancellationToken ct) =>
        service.ReactivateAsync(nic, request, ActorId, ct);
}
