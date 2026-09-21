using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Controllers;

[ApiController, Route("api/v1/stations")]
public sealed class StationsController(StationService service) : ControllerBase
{
    private bool Staff => User.IsInRole(Roles.Backoffice) || User.IsInRole(Roles.GridOperator);
    [HttpGet]
    public Task<PageResponse<StationResponse>> List(bool activeOnly = false, [Range(1, 100000)] int page = 1,
        [Range(1, 100)] int pageSize = 20, [Range(-90d, 90d)] double? latitude = null,
        [Range(-180d, 180d)] double? longitude = null, [Range(0.1, 500)] double radiusKm = 25, CancellationToken ct = default) =>
        service.ListAsync(Staff, activeOnly, page, pageSize, latitude, longitude, radiusKm, ct);
    [HttpGet("{id}")]
    public Task<StationResponse> Get(string id, CancellationToken ct) => service.GetAsync(id, Staff, ct);
    [HttpPost, Authorize(Roles = Roles.Backoffice)]
    public async Task<ActionResult<StationResponse>> Create(StationInput request, CancellationToken ct)
    {
        var station = await service.CreateAsync(request, ct);
        return CreatedAtAction(nameof(Get), new { id = station.Id }, station);
    }
    [HttpPatch("{id}"), Authorize(Roles = Roles.Backoffice)]
    public Task<StationResponse> Update(string id, StationUpdate request, CancellationToken ct) => service.UpdateAsync(id, request, ct);
    [HttpPost("{id}/status"), Authorize(Roles = Roles.Backoffice)]
    public Task<StationResponse> Status(string id, StationStatusInput request, CancellationToken ct) => service.StatusAsync(id, request, ct);
    [HttpPut("{id}/schedule"), Authorize(Roles = Roles.Staff)]
    public Task<StationResponse> Schedule(string id, ScheduleInput request, CancellationToken ct) => service.ScheduleAsync(id, request, ct);
    [HttpPost("{id}/slots"), Authorize(Roles = Roles.Staff)]
    public Task<StationResponse> AddSlot(string id, SlotInput request, CancellationToken ct) => service.AddSlotAsync(id, request, ct);
    [HttpPut("{id}/slots/{slotId}/availability"), Authorize(Roles = Roles.Staff)]
    public Task<StationResponse> Availability(string id, string slotId, AvailabilityInput request, CancellationToken ct) => service.AvailabilityAsync(id, slotId, request, ct);
    [HttpPost("{id}/slots/{slotId}/archive"), Authorize(Roles = Roles.Staff)]
    public Task<StationResponse> Archive(string id, string slotId, StationVersion request, CancellationToken ct) => service.RemoveSlotAsync(id, slotId, request, ct);
    [HttpPost("{id}/slots/{slotId}/allocations"), Authorize(Roles = Roles.Staff)]
    public Task<StationResponse> Reserve(string id, string slotId, AllocationInput request, CancellationToken ct) => service.ReserveAsync(id, slotId, request, ct);
    [HttpPost("{id}/slots/{slotId}/allocations/{bookingId}/cancel"), Authorize(Roles = Roles.Staff)]
    public Task<StationResponse> Cancel(string id, string slotId, string bookingId, StationVersion request, CancellationToken ct) => service.EndAllocationAsync(id, slotId, bookingId, request, false, ct);
    [HttpPost("{id}/slots/{slotId}/allocations/{bookingId}/complete"), Authorize(Roles = Roles.Staff)]
    public Task<StationResponse> Complete(string id, string slotId, string bookingId, StationVersion request, CancellationToken ct) => service.EndAllocationAsync(id, slotId, bookingId, request, true, ct);
}
