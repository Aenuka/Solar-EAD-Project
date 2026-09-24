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
    /// Dashboard with counts.
    /// </summary>
    public async Task<IActionResult> Dashboard(CancellationToken ct)
    {
        try
        {
            var pending = await _api.GetPendingReservationsAsync(ct);
            var approvedFuture = await _api.GetApprovedFutureCountAsync(ct);

            ViewBag.PendingCount = pending.Count;
            ViewBag.ApprovedFutureCount = approvedFuture.Count;
            return View();
        }
        catch (Exception ex)
        {
            ViewBag.Error = ex.Message;
            ViewBag.PendingCount = 0;
            ViewBag.ApprovedFutureCount = 0;
            return View();
        }
    }
}