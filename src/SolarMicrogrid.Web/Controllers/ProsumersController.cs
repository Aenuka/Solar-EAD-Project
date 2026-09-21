using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;
using SolarMicrogrid.Web.ViewModels;

namespace SolarMicrogrid.Web.Controllers;

[Authorize(Roles = Roles.Backoffice)]
public sealed class ProsumersController(MicrogridApiClient api) : Controller
{
    [HttpGet]
    public async Task<IActionResult> Index(string? search, AccountStatus? status, bool pending = false, int page = 1, CancellationToken ct = default)
    {
        ViewBag.Search = search; ViewBag.Status = status; ViewBag.Pending = pending;
        var path = $"prosumers?page={page}&search={Uri.EscapeDataString(search ?? "")}";
        if (status.HasValue) path += $"&status={status}";
        if (pending) path += "&requestStatus=Pending";
        return View(await api.GetAsync<PageResponse<ProsumerResponse>>(path, ct));
    }

    [HttpGet]
    public async Task<IActionResult> Details(string id, CancellationToken ct)
    {
        var user = await GetAsync(id, ct);
        return View(new ProsumerEditViewModel
        { Nic = user.Nic, FullName = user.FullName, Email = user.Email, Phone = user.Phone, Address = user.Address, Version = user.Version, Account = user });
    }

    [HttpPost]
    public async Task<IActionResult> Details(string id, ProsumerEditViewModel model, CancellationToken ct)
    {
        model.Nic = id;
        if (ModelState.IsValid)
        {
            try
            {
                await api.PatchAsync<ProsumerResponse>($"prosumers/{Uri.EscapeDataString(id)}",
                    new UpdateProfileRequest(model.FullName, model.Email, model.Phone, model.Address, model.Version), ct);
                TempData["Success"] = "Profile updated.";
                return RedirectToAction(nameof(Details), new { id });
            }
            catch (ApiFailureException e) when (e.StatusCode is 400 or 409) { ModelState.AddModelError("", e.Message); }
        }
        model.Account = await GetAsync(id, ct);
        return View(model);
    }

    [HttpPost]
    public async Task<IActionResult> Decide(string id, string requestId, DecisionRequest request, CancellationToken ct)
    {
        if (!ModelState.IsValid) TempData["Error"] = "Choose a decision and enter a note of 5–500 characters.";
        else
        {
            try
            {
                await api.PostAsync<ProsumerResponse>($"prosumers/{Uri.EscapeDataString(id)}/deactivation-requests/{Uri.EscapeDataString(requestId)}/decision", request, ct);
                TempData["Success"] = "Deactivation request reviewed.";
            }
            catch (ApiFailureException e) when (e.StatusCode is 400 or 409) { TempData["Error"] = e.Message; }
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpPost]
    public async Task<IActionResult> Reactivate(string id, ReactivateRequest request, CancellationToken ct)
    {
        if (!ModelState.IsValid) TempData["Error"] = "Enter a reactivation note of 5–500 characters.";
        else
        {
            try
            {
                await api.PostAsync<ProsumerResponse>($"prosumers/{Uri.EscapeDataString(id)}/reactivation", request, ct);
                TempData["Success"] = "Account reactivated. The prosumer can now sign in again.";
            }
            catch (ApiFailureException e) when (e.StatusCode is 400 or 409) { TempData["Error"] = e.Message; }
        }
        return RedirectToAction(nameof(Details), new { id });
    }

    private Task<ProsumerResponse> GetAsync(string id, CancellationToken ct) =>
        api.GetAsync<ProsumerResponse>($"prosumers/{Uri.EscapeDataString(id)}", ct);
}
