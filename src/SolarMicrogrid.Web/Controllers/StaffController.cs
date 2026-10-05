/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Serves Backoffice staff-management pages and submits staff creation, profile, role, and status changes to the API.
 */

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Web.Presentation;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;
using SolarMicrogrid.Web.ViewModels;

namespace SolarMicrogrid.Web.Controllers;

[Authorize(Roles = Roles.Backoffice)]
public sealed class StaffController(MicrogridApiClient api) : PortalController
{
    [HttpGet]
    public async Task<IActionResult> Index(int page = 1, CancellationToken ct = default)
    {
        var staff = await api.GetAsync<PageResponse<StaffResponse>>($"staff-users?page={page}", ct);
        return ReactPage(staff);
    }

    [HttpGet]
    public IActionResult Create() => ReactPage(new StaffCreateViewModel());

    [HttpPost]
    public async Task<IActionResult> Create(StaffCreateViewModel model, CancellationToken ct)
    {
        if (!ModelState.IsValid)
        {
            return ReactPage(model);
        }

        try
        {
            var request = new CreateStaffRequest(model.Username, model.FullName, model.Email, model.Password, model.Role);
            await api.PostAsync<StaffResponse>("staff-users", request, ct);
            TempData["Success"] = "Staff account created.";
            return RedirectToAction(nameof(Index));
        }
        catch (ApiFailureException exception) when (exception.StatusCode is 400 or 409)
        {
            AddErrors(exception);
            return ReactPage(model);
        }
    }

    [HttpGet]
    public async Task<IActionResult> Edit(string id, CancellationToken ct)
    {
        var user = await api.GetAsync<StaffResponse>($"staff-users/{Uri.EscapeDataString(id)}", ct);
        var model = new StaffEditViewModel
        {
            Id = user.Id,
            Username = user.Username,
            FullName = user.FullName,
            Email = user.Email,
            IsProtected = user.IsProtected,
            Role = Enum.Parse<StaffRole>(user.Role),
            Status = user.Status,
            Version = user.Version
        };
        return ReactPage(model);
    }

    [HttpPost]
    public async Task<IActionResult> Edit(string id, StaffEditViewModel model, CancellationToken ct)
    {
        model.Id = id;
        if (ModelState.IsValid)
        {
            try
            {
                var request = new UpdateStaffRequest(model.FullName, model.Email, model.Role, model.Status, model.Version);
                await api.PatchAsync<StaffResponse>($"staff-users/{Uri.EscapeDataString(id)}", request, ct);
                TempData["Success"] = "Staff account updated.";
                return RedirectToAction(nameof(Index));
            }
            catch (ApiFailureException exception) when (exception.StatusCode is 400 or 403 or 409)
            {
                AddErrors(exception);
            }
        }

        var user = await api.GetAsync<StaffResponse>($"staff-users/{Uri.EscapeDataString(id)}", ct);
        model.Username = user.Username;
        model.IsProtected = user.IsProtected;
        return ReactPage(model);
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
}
