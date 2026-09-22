using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;
using SolarMicrogrid.Web.ViewModels;

namespace SolarMicrogrid.Web.Controllers;

[Authorize(Roles = Roles.Staff)]
public sealed class StationsController(MicrogridApiClient api) : Controller
{
    private static string PathFor(string id) => "stations/" + Uri.EscapeDataString(id);
    [HttpGet]
    public async Task<IActionResult> Index(int page = 1, CancellationToken ct = default) =>
        View(await api.GetAsync<PageResponse<StationResponse>>($"stations?page={page}", ct));
    [HttpGet]
    public async Task<IActionResult> Details(string id, CancellationToken ct) => View(await api.GetAsync<StationResponse>(PathFor(id), ct));
    [HttpGet, Authorize(Roles = Roles.Backoffice)]
    public IActionResult Create() => View(new StationInput());
    [HttpPost, Authorize(Roles = Roles.Backoffice)]
    public async Task<IActionResult> Create(StationInput model, CancellationToken ct)
    {
        if (ModelState.IsValid)
            try
            {
                var result = await api.PostAsync<StationResponse>("stations", model, ct);
                TempData["Success"] = "Station created. Set its schedule and add energy windows below.";
                return RedirectToAction(nameof(Details), new { id = result.Id });
            }
            catch (ApiFailureException e) when (e.StatusCode is 400 or 409) { Errors(e); }
        return View(model);
    }
    [HttpGet, Authorize(Roles = Roles.Backoffice)]
    public async Task<IActionResult> Edit(string id, CancellationToken ct)
    {
        var s = await api.GetAsync<StationResponse>(PathFor(id), ct);
        return View(new StationUpdate { Name = s.Name, Address = s.Address, Latitude = s.Latitude, Longitude = s.Longitude,
            CapacityKw = s.CapacityKw, StorageKwh = s.StorageKwh, BatterySlots = s.BatterySlots, Version = s.Version });
    }
    [HttpPost, Authorize(Roles = Roles.Backoffice)]
    public async Task<IActionResult> Edit(string id, StationUpdate model, CancellationToken ct)
    {
        if (ModelState.IsValid)
            try
            {
                await api.PatchAsync<StationResponse>(PathFor(id), model, ct);
                TempData["Success"] = "Station updated.";
                return RedirectToAction(nameof(Details), new { id });
            }
            catch (ApiFailureException e) when (e.StatusCode is 400 or 409) { Errors(e); }
        return View(model);
    }
    [HttpPost, Authorize(Roles = Roles.Backoffice)]
    public Task<IActionResult> Status(string id, StationStatusInput model, CancellationToken ct) =>
        Change(id, () => api.PostAsync<StationResponse>(PathFor(id) + "/status", model, ct));
    [HttpPost]
    public Task<IActionResult> Schedule(string id, ScheduleInput model, CancellationToken ct) =>
        Change(id, () => api.PutAsync<StationResponse>(PathFor(id) + "/schedule", model, ct));
    [HttpPost]
    public Task<IActionResult> AddSlot(string id, StationSlotForm model, CancellationToken ct) =>
        Change(id, () => api.PostAsync<StationResponse>(PathFor(id) + "/slots", model.ToRequest(), ct));
    [HttpPost]
    public Task<IActionResult> Availability(string id, string slotId, AvailabilityInput model, CancellationToken ct) =>
        Change(id, () => api.PutAsync<StationResponse>(PathFor(id) + "/slots/" + Uri.EscapeDataString(slotId) + "/availability", model, ct));
    [HttpPost]
    public Task<IActionResult> Archive(string id, string slotId, StationVersion model, CancellationToken ct) =>
        Change(id, () => api.PostAsync<StationResponse>(PathFor(id) + "/slots/" + Uri.EscapeDataString(slotId) + "/archive", model, ct));
    private async Task<IActionResult> Change(string id, Func<Task<StationResponse>> operation)
    {
        if (!ModelState.IsValid)
            TempData["Error"] = string.Join(" ", ModelState.Values.SelectMany(x => x.Errors).Select(x => string.IsNullOrEmpty(x.ErrorMessage) ? "Check the entered values." : x.ErrorMessage));
        else
            try { await operation(); TempData["Success"] = "Station updated."; }
            catch (ApiFailureException e) when (e.StatusCode is 400 or 409) { TempData["Error"] = e.Message; }
        return RedirectToAction(nameof(Details), new { id });
    }
    private void Errors(ApiFailureException e)
    {
        ModelState.AddModelError("", e.Message);
        foreach (var field in e.Errors) foreach (var error in field.Value) ModelState.AddModelError(field.Key, error);
    }
}
