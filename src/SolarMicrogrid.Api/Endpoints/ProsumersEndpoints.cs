using System.ComponentModel.DataAnnotations;
using System.Security.Claims;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Endpoints;

// Reference: Julio Casal YouTube tutorials
// https://www.youtube.com/@juliocasal
public static class ProsumersEndpoints
{
    private const string GetMyProfileEndpointName = "GetMyProfile";

    public static void MapProsumersEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/v1/prosumers").WithTags("Prosumers");

        // POST /api/v1/prosumers
        group.MapPost("", async (RegisterProsumerRequest request, ProsumerService service, CancellationToken ct) =>
        {
            var prosumer = await service.RegisterAsync(request, ct);
            return Results.CreatedAtRoute(GetMyProfileEndpointName, value: prosumer);
        })
            .WithJsonBody<RegisterProsumerRequest>()
            .Produces<ProsumerResponse>(StatusCodes.Status201Created)
            .AllowAnonymous()
            .RequireRateLimiting("auth");

        // These routes get the NIC from the signed-in user's token.
        var mine = group.MapGroup("/me")
            .RequireAuthorization(policy => policy.RequireRole(Roles.Prosumer));

        // GET /api/v1/prosumers/me
        mine.MapGet("", (ClaimsPrincipal user, ProsumerService service, CancellationToken ct) =>
            service.GetAsync(user.FindFirstValue("sub")!, ct))
            .WithName(GetMyProfileEndpointName);

        // PATCH /api/v1/prosumers/me
        mine.MapPatch("", (UpdateProfileRequest request, ClaimsPrincipal user,
            ProsumerService service, CancellationToken ct) =>
        {
            var nic = user.FindFirstValue("sub")!;
            return service.UpdateProfileAsync(nic, request, nic, ct);
        })
            .WithJsonBody<UpdateProfileRequest>();

        // POST /api/v1/prosumers/me/deactivation-requests
        mine.MapPost("/deactivation-requests", async (DeactivationRequestInput request,
            ClaimsPrincipal user, ProsumerService service, CancellationToken ct) =>
        {
            var prosumer = await service.RequestDeactivationAsync(user.FindFirstValue("sub")!, request, ct);
            return Results.CreatedAtRoute(GetMyProfileEndpointName, value: prosumer);
        })
            .WithJsonBody<DeactivationRequestInput>()
            .Produces<ProsumerResponse>(StatusCodes.Status201Created);

        var backoffice = group.MapGroup("")
            .RequireAuthorization(policy => policy.RequireRole(Roles.Backoffice));

        // GET /api/v1/prosumers
        backoffice.MapGet("", (ProsumerService service, CancellationToken ct,
            [Range(1, 100000)] int page = 1, [Range(1, 100)] int pageSize = 20,
            [EnumDataType(typeof(AccountStatus))] AccountStatus? status = null,
            [EnumDataType(typeof(RequestStatus))] RequestStatus? requestStatus = null,
            [StringLength(100)] string? search = null) =>
            service.ListAsync(page, pageSize, status, requestStatus, search, ct));

        // GET /api/v1/prosumers/{nic}
        backoffice.MapGet("/{nic}", (string nic, ProsumerService service, CancellationToken ct) =>
            service.GetAsync(nic, ct));

        // PATCH /api/v1/prosumers/{nic}
        backoffice.MapPatch("/{nic}", (string nic, UpdateProfileRequest request, ClaimsPrincipal user,
            ProsumerService service, CancellationToken ct) =>
            service.UpdateProfileAsync(nic, request, user.FindFirstValue("sub")!, ct))
            .WithJsonBody<UpdateProfileRequest>();

        // POST /api/v1/prosumers/{nic}/deactivation-requests/{requestId}/decision
        backoffice.MapPost("/{nic}/deactivation-requests/{requestId}/decision",
            (string nic, string requestId, DecisionRequest request, ClaimsPrincipal user,
                ProsumerService service, CancellationToken ct) =>
                service.DecideAsync(nic, requestId, request, user.FindFirstValue("sub")!, ct))
            .WithJsonBody<DecisionRequest>();

        // POST /api/v1/prosumers/{nic}/reactivation
        backoffice.MapPost("/{nic}/reactivation", (string nic, ReactivateRequest request,
            ClaimsPrincipal user, ProsumerService service, CancellationToken ct) =>
            service.ReactivateAsync(nic, request, user.FindFirstValue("sub")!, ct))
            .WithJsonBody<ReactivateRequest>();
    }
}
