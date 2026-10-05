// File: StationsEndpoints.cs
// Purpose: Maps station HTTP routes and applies staff permissions.
// Group member responsible: Chamithu Edirimanna (IT23202054).

using System.ComponentModel.DataAnnotations;
using System.Security.Claims;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Endpoints;

public static class StationsEndpoints
{
    private const string GetStationEndpointName = "GetStation";

    // Registers station discovery, management, and allocation endpoints.
    public static void MapStationsEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/v1/stations").WithTags("Stations");

        // GET /api/v1/stations
        group.MapGet("", (ClaimsPrincipal user, StationService service, CancellationToken ct,
            bool activeOnly = false, [Range(1, 100000)] int page = 1, [Range(1, 100)] int pageSize = 20,
            [Range(-90d, 90d)] double? latitude = null, [Range(-180d, 180d)] double? longitude = null,
            [Range(0.1, 500)] double radiusKm = 25) =>
            service.ListAsync(IsStaff(user), activeOnly, page, pageSize, latitude, longitude, radiusKm, ct));

        // GET /api/v1/stations/{id}
        group.MapGet("/{id}", (string id, ClaimsPrincipal user, StationService service, CancellationToken ct) =>
            service.GetAsync(id, IsStaff(user), ct))
            .WithName(GetStationEndpointName);

        // POST /api/v1/stations
        group.MapPost("", async (StationInput request, StationService service, CancellationToken ct) =>
        {
            var station = await service.CreateAsync(request, ct);
            return Results.CreatedAtRoute(GetStationEndpointName, new { id = station.Id }, station);
        })
            .WithJsonBody<StationInput>()
            .Produces<StationResponse>(StatusCodes.Status201Created)
            .RequireAuthorization(policy => policy.RequireRole(Roles.Backoffice));

        // PATCH /api/v1/stations/{id}
        group.MapPatch("/{id}", (string id, StationUpdate request, StationService service, CancellationToken ct) =>
            service.UpdateAsync(id, request, ct))
            .WithJsonBody<StationUpdate>()
            .RequireAuthorization(policy => policy.RequireRole(Roles.Backoffice));

        // POST /api/v1/stations/{id}/status
        group.MapPost("/{id}/status", (string id, StationStatusInput request, StationService service, CancellationToken ct) =>
            service.StatusAsync(id, request, ct))
            .WithJsonBody<StationStatusInput>()
            .RequireAuthorization(policy => policy.RequireRole(Roles.Backoffice));

        var staff = group.MapGroup("")
            .RequireAuthorization(policy => policy.RequireRole(Roles.Backoffice, Roles.GridOperator));

        // PUT /api/v1/stations/{id}/schedule
        staff.MapPut("/{id}/schedule", (string id, ScheduleInput request, StationService service, CancellationToken ct) =>
            service.ScheduleAsync(id, request, ct))
            .WithJsonBody<ScheduleInput>();

        // POST /api/v1/stations/{id}/slots
        staff.MapPost("/{id}/slots", (string id, SlotInput request, StationService service, CancellationToken ct) =>
            service.AddSlotAsync(id, request, ct))
            .WithJsonBody<SlotInput>();

        // PUT /api/v1/stations/{id}/slots/{slotId}/availability
        staff.MapPut("/{id}/slots/{slotId}/availability", (string id, string slotId,
            AvailabilityInput request, StationService service, CancellationToken ct) =>
            service.AvailabilityAsync(id, slotId, request, ct))
            .WithJsonBody<AvailabilityInput>();

        // POST /api/v1/stations/{id}/slots/{slotId}/archive
        staff.MapPost("/{id}/slots/{slotId}/archive", (string id, string slotId,
            StationVersion request, StationService service, CancellationToken ct) =>
            service.RemoveSlotAsync(id, slotId, request, ct))
            .WithJsonBody<StationVersion>();

        // POST /api/v1/stations/{id}/slots/{slotId}/allocations
        staff.MapPost("/{id}/slots/{slotId}/allocations", (string id, string slotId,
            AllocationInput request, StationService service, CancellationToken ct) =>
            service.ReserveAsync(id, slotId, request, ct))
            .WithJsonBody<AllocationInput>();

        // POST /api/v1/stations/{id}/slots/{slotId}/allocations/{bookingId}/cancel
        staff.MapPost("/{id}/slots/{slotId}/allocations/{bookingId}/cancel", (string id, string slotId,
            string bookingId, StationVersion request, StationService service, CancellationToken ct) =>
            service.EndAllocationAsync(id, slotId, bookingId, request, false, ct))
            .WithJsonBody<StationVersion>();

        // POST /api/v1/stations/{id}/slots/{slotId}/allocations/{bookingId}/complete
        staff.MapPost("/{id}/slots/{slotId}/allocations/{bookingId}/complete", (string id, string slotId,
            string bookingId, StationVersion request, StationService service, CancellationToken ct) =>
            service.EndAllocationAsync(id, slotId, bookingId, request, true, ct))
            .WithJsonBody<StationVersion>();
    }

    // Checks whether the signed-in user can access staff station details.
    private static bool IsStaff(ClaimsPrincipal user) =>
        user.IsInRole(Roles.Backoffice) || user.IsInRole(Roles.GridOperator);
}
