/*
 * File: OperatorController.cs
 * Author: Pasindu
 * Description: Web UI for Operator Dashboard, showing pending and approved future reservations.
 */

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;

namespace SolarMicrogrid.Web.Controllers;

[Authorize(Roles = "GridOperator")]
public class OperatorController : Controller
{
    private readonly MicrogridApiClient _api;

    public OperatorController(MicrogridApiClient api)
    {
        _api = api;
    }

    /// <summary>
    /// Dashboard with live counts and recent activity for the Operator.
    /// </summary>
    public async Task<IActionResult> Dashboard(CancellationToken ct)
    {
        try
        {
            var pending = await _api.GetPendingReservationsAsync(ct);
            var allBookings = await _api.SearchReservationsAsync(null, null, null, ct) 
                              ?? new List<ReservationResponse>();

            var now = DateTime.UtcNow;

            ViewBag.PendingCount = pending.Count;
            ViewBag.ApprovedFutureCount = allBookings.Count(b => 
                b.Status == "APPROVED" && b.ReservationDate > now);
            ViewBag.CompletedCount = allBookings.Count(b => b.Status == "COMPLETED");
            
            ViewBag.PendingReservations = pending.Take(5).ToList();
            ViewBag.RecentCompleted = allBookings
                .Where(b => b.Status == "COMPLETED")
                .OrderByDescending(b => b.CompletedAt ?? b.UpdatedAt)
                .Take(5)
                .ToList();

            return View();
        }
        catch (Exception ex)
        {
            ViewBag.Error = ex.Message;
            ViewBag.PendingCount = 0;
            ViewBag.ApprovedFutureCount = 0;
            ViewBag.CompletedCount = 0;
            ViewBag.PendingReservations = new List<ReservationResponse>();
            ViewBag.RecentCompleted = new List<ReservationResponse>();
            return View();
        }
    }
}
