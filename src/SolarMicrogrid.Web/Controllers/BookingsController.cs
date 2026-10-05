/*
 * File: BookingsController.cs
 * Author: Sajith
 * Description: Web UI for operator booking monitoring.
 */

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Web.Presentation;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;

namespace SolarMicrogrid.Web.Controllers;

[Authorize(Roles = "Backoffice,GridOperator")]
public class BookingsController : PortalController
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
            PageMeta["FilterStatus"] = status;
            PageMeta["FilterStation"] = stationId;
            var stations = new List<StationResponse>();
            for (var page = 1; ; page++)
            {
                var response = await _api.GetAsync<PageResponse<StationResponse>>($"stations?page={page}&pageSize=100", ct);
                stations.AddRange(response.Items);
                if (stations.Count >= response.Total || response.Items.Count == 0) break;
            }
            PageMeta["Stations"] = stations.Select(s => new { s.Id, s.Name, s.Address, s.Active });
            return ReactPage(bookings);
        }
        catch (Exception ex)
        {
            PageMeta["Error"] = ex.Message;
            return ReactPage(new List<ReservationResponse>());
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
            return ReactPage(bookings);
        }
        catch (Exception ex)
        {
            PageMeta["Error"] = ex.Message;
            return ReactPage(new List<ReservationResponse>());
        }
    }

    /// <summary>
    /// Approves a pending reservation.
    /// </summary>
    [HttpPost]
    public async Task<IActionResult> Approve(string id, CancellationToken ct)
    {
        try
        {
            await _api.ApproveReservationAsync(id, ct);
            return RedirectToAction(nameof(Pending));
        }
        catch (Exception ex)
        {
            // Preserve the error for React after the redirect.
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

            PageMeta["PendingCount"] = allBookings.Count(b => b.Status == "PENDING");
            PageMeta["ApprovedFutureCount"] = allBookings.Count(b =>
                b.Status == "APPROVED" && b.ReservationDate > now);
            PageMeta["TotalCount"] = allBookings.Count;
            PageMeta["CompletedCount"] = allBookings.Count(b => b.Status == "COMPLETED");
            PageMeta["RecentBookings"] = allBookings
                .OrderByDescending(b => b.CreatedAt)
                .Take(5)
                .ToList();

            return ReactPage();
        }
        catch (Exception ex)
        {
            PageMeta["Error"] = ex.Message;
            PageMeta["PendingCount"] = 0;
            PageMeta["ApprovedFutureCount"] = 0;
            PageMeta["TotalCount"] = 0;
            PageMeta["CompletedCount"] = 0;
            PageMeta["RecentBookings"] = new List<ReservationResponse>();
            return ReactPage();
        }
    }
}
