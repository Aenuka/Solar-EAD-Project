/*
 * File: BookingsController.cs
 * Author: Sajith
 * Description: Web UI for operator booking monitoring.
 */

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;

namespace SolarMicrogrid.Web.Controllers;

[Authorize(Roles = "Backoffice,GridOperator")]
public class BookingsController : Controller
{
    private readonly MicrogridApiClient _api;

    public BookingsController(MicrogridApiClient api)
    {
        _api = api;
    }

    /// <summary>
    /// Displays all reservations with optional filters.
    /// </summary>
    public async Task<IActionResult> Index(string? status, string? stationId, CancellationToken ct)
    {
        try
        {
            var bookings = await _api.SearchReservationsAsync(status, stationId, null, ct);
            ViewBag.FilterStatus = status;
            ViewBag.FilterStation = stationId;
            return View(bookings);
        }
        catch (Exception ex)
        {
            ViewBag.Error = ex.Message;
            return View(new List<ReservationResponse>());
        }
    }

    /// <summary>
    /// Displays only pending reservations.
    /// </summary>
    public async Task<IActionResult> Pending(CancellationToken ct)
    {
        try
        {
            var bookings = await _api.GetPendingReservationsAsync(ct);
            return View(bookings);
        }
        catch (Exception ex)
        {
            ViewBag.Error = ex.Message;
            return View(new List<ReservationResponse>());
        }
    }

    /// <summary>
    /// Approves a pending reservation.
    /// </summary>
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Approve(string id, CancellationToken ct)
    {
        try
        {
            await _api.ApproveReservationAsync(id, ct);
            return RedirectToAction(nameof(Pending));
        }
        catch (Exception ex)
        {
            // Instead of TempData or complex error handling, we can just log or pass error
            // to a view. Since it's a redirect, we could use TempData for a simple error message.
            TempData["Error"] = ex.Message;
            return RedirectToAction(nameof(Pending));
        }
    }

    /// <summary>
    /// Dashboard with live counts and recent activity.
    /// </summary>
    public async Task<IActionResult> Dashboard(CancellationToken ct)
    {
        try
        {
            // Fetch all bookings once, then compute stats locally
            var allBookings = await _api.SearchReservationsAsync(null, null, null, ct) 
                              ?? new List<ReservationResponse>();

            var now = DateTime.UtcNow;

            ViewBag.PendingCount = allBookings.Count(b => b.Status == "PENDING");
            ViewBag.ApprovedFutureCount = allBookings.Count(b => 
                b.Status == "APPROVED" && b.ReservationDate > now);
            ViewBag.TotalCount = allBookings.Count;
            ViewBag.CompletedCount = allBookings.Count(b => b.Status == "COMPLETED");
            ViewBag.RecentBookings = allBookings
                .OrderByDescending(b => b.CreatedAt)
                .Take(5)
                .ToList();

            return View();
        }
        catch (Exception ex)
        {
            ViewBag.Error = ex.Message;
            ViewBag.PendingCount = 0;
            ViewBag.ApprovedFutureCount = 0;
            ViewBag.TotalCount = 0;
            ViewBag.CompletedCount = 0;
            ViewBag.RecentBookings = new List<ReservationResponse>();
            return View();
        }
    }
}