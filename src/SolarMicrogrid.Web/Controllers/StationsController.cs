using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Web.Presentation;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;
using SolarMicrogrid.Web.ViewModels;

namespace SolarMicrogrid.Web.Controllers;

[Authorize(Roles = Roles.Staff)]
public sealed class StationsController(MicrogridApiClient api) : PortalController
{
    [HttpGet]
    public async Task<IActionResult> Index(int page = 1, CancellationToken ct = default)
    {
        var stations = await api.GetAsync<PageResponse<StationResponse>>($"stations?page={page}", ct);
        return ReactPage(stations);
    }

    [HttpGet]
    public async Task<IActionResult> Details(string id, CancellationToken ct)
    {
        var station = await api.GetAsync<StationResponse>(StationPath(id), ct);
        return ReactPage(station);
    }

    [HttpGet, Authorize(Roles = Roles.Backoffice)]
    public IActionResult Create() => ReactPage(new StationInput());

    [HttpPost, Authorize(Roles = Roles.Backoffice)]
    public async Task<IActionResult> Create(StationInput model, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return ReactPage(model);
        }

        try
        {
            var station = await api.PostAsync<StationResponse>("stations", model, ct);
            TempData["Success"] = "Station created. Set its schedule and add energy windows below.";
            return RedirectToAction(nameof(Details), new { id = station.Id });
        }
        catch (ApiFailureException exception) when (exception.StatusCode is 400 or 409)
        {
            AddErrors(exception);
            return ReactPage(model);
        }
    }

    [HttpGet, Authorize(Roles = Roles.Backoffice)]
    public async Task<IActionResult> Edit(string id, CancellationToken ct)
    {
        var station = await api.GetAsync<StationResponse>(StationPath(id), ct);
        var model = new StationUpdate
        {
            Name = station.Name,
            Address = station.Address,
            Latitude = station.Latitude,
            Longitude = station.Longitude,
            CapacityKw = station.CapacityKw,
            StorageKwh = station.StorageKwh,
            BatterySlots = station.BatterySlots,
            Version = station.Version
        };
        return ReactPage(model);
    }

    [HttpPost, Authorize(Roles = Roles.Backoffice)]
    public async Task<IActionResult> Edit(string id, StationUpdate model, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return ReactPage(model);
        }

        try
        {
            await api.PatchAsync<StationResponse>(StationPath(id), model, ct);
            TempData["Success"] = "Station updated.";
            return RedirectToAction(nameof(Details), new { id });
        }
        catch (ApiFailureException exception) when (exception.StatusCode is 400 or 409)
        {
            AddErrors(exception);
            return ReactPage(model);
        }
    }

    [HttpPost, Authorize(Roles = Roles.Backoffice)]
    public Task<IActionResult> Status(string id, StationStatusInput model, CancellationToken ct) =>
        SaveStationChangeAsync(id, () => api.PostAsync<StationResponse>(StationPath(id) + "/status", model, ct));

    [HttpPost]
    public Task<IActionResult> Schedule(string id, ScheduleInput model, CancellationToken ct) =>
        SaveStationChangeAsync(id, () => api.PutAsync<StationResponse>(StationPath(id) + "/schedule", model, ct));

    [HttpPost]
    public Task<IActionResult> AddSlot(string id, StationSlotForm model, CancellationToken ct) =>
        SaveStationChangeAsync(id, () => api.PostAsync<StationResponse>(StationPath(id) + "/slots", model.ToRequest(), ct));

    [HttpPost]
    public Task<IActionResult> Availability(string id, string slotId, AvailabilityInput model, CancellationToken ct) =>
        SaveStationChangeAsync(id, () => api.PutAsync<StationResponse>(
            StationPath(id) + "/slots/" + Uri.EscapeDataString(slotId) + "/availability", model, ct));

    [HttpPost]
    public Task<IActionResult> Archive(string id, string slotId, StationVersion model, CancellationToken ct) =>
        SaveStationChangeAsync(id, () => api.PostAsync<StationResponse>(
            StationPath(id) + "/slots/" + Uri.EscapeDataString(slotId) + "/archive", model, ct));

    private async Task<IActionResult> SaveStationChangeAsync(string id, Func<Task<StationResponse>> save)
    {
        if (!ModelState.IsValid)
        {
            var errors = ModelState.Values.SelectMany(value => value.Errors)
                .Select(error => string.IsNullOrEmpty(error.ErrorMessage) ? "Check the entered values." : error.ErrorMessage);
            TempData["Error"] = string.Join(" ", errors);
            return RedirectToAction(nameof(Details), new { id });
        }

        // Invoke the supplied API call only after validating the form.
        try
        {
            await save();
            TempData["Success"] = "Station updated.";
        }
        catch (ApiFailureException exception) when (exception.StatusCode is 400 or 409)
        {
            TempData["Error"] = exception.Message;
        }

        return RedirectToAction(nameof(Details), new { id });
    }

    private void AddErrors(ApiFailureException exception)
    {
        ModelState.AddModelError("", exception.Message);
        foreach (var (field, errors) in exception.Errors)
        {
            foreach (var message in errors)
            {
                ModelState.AddModelError(field, message);
            }
        }
    }

    private static string StationPath(string id) => "stations/" + Uri.EscapeDataString(id);
}
