using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;
using SolarMicrogrid.Web.ViewModels;

namespace SolarMicrogrid.Web.Controllers;

[Authorize(Roles = Roles.Backoffice)]
public sealed class StaffController(MicrogridApiClient api) : Controller
{
    [HttpGet]
    public async Task<IActionResult> Index(int page = 1, CancellationToken ct = default) =>
        View(await api.GetAsync<PageResponse<StaffResponse>>($"staff-users?page={page}", ct));

    [HttpGet] public IActionResult Create() => View(new StaffCreateViewModel());

    [HttpPost]
    public async Task<IActionResult> Create(StaffCreateViewModel model, CancellationToken ct)
    {
        if (!ModelState.IsValid) return View(model);
        try
        {
            await api.PostAsync<StaffResponse>("staff-users", new CreateStaffRequest(model.Username, model.FullName, model.Email, model.Password, model.Role), ct);
            TempData["Success"] = "Staff account created.";
            return RedirectToAction(nameof(Index));
        }
        catch (ApiFailureException e) when (e.StatusCode is 400 or 409) { AddErrors(e); return View(model); }
    }

    [HttpGet]
    public async Task<IActionResult> Edit(string id, CancellationToken ct)
    {
        var user = await api.GetAsync<StaffResponse>($"staff-users/{Uri.EscapeDataString(id)}", ct);
        return View(new StaffEditViewModel
        {
            Id = user.Id, Username = user.Username, FullName = user.FullName, Email = user.Email, IsProtected = user.IsProtected,
            Role = Enum.Parse<StaffRole>(user.Role), Status = user.Status, Version = user.Version
        });
    }

    [HttpPost]
    public async Task<IActionResult> Edit(string id, StaffEditViewModel model, CancellationToken ct)
    {
        model.Id = id;
        if (ModelState.IsValid)
        {
            try
            {
                await api.PatchAsync<StaffResponse>($"staff-users/{Uri.EscapeDataString(id)}",
                    new UpdateStaffRequest(model.FullName, model.Email, model.Role, model.Status, model.Version), ct);
                TempData["Success"] = "Staff account updated.";
                return RedirectToAction(nameof(Index));
            }
            catch (ApiFailureException e) when (e.StatusCode is 400 or 403 or 409) { AddErrors(e); }
        }
        var user = await api.GetAsync<StaffResponse>($"staff-users/{Uri.EscapeDataString(id)}", ct);
        model.Username = user.Username;
        model.IsProtected = user.IsProtected;
        return View(model);
    }

    private void AddErrors(ApiFailureException error)
    {
        ModelState.AddModelError("", error.Message);
        foreach (var (field, errors) in error.Errors) foreach (var message in errors) ModelState.AddModelError(field, message);
    }
}
