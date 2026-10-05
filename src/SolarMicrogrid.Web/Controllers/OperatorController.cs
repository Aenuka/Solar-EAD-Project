/*
 * File: OperatorController.cs
 * Author: Pasindu
 * Description: Web UI for Operator Dashboard, showing pending and approved future reservations.
 */

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Contracts;
using SolarMicrogrid.Web.ApiClients;
using SolarMicrogrid.Web.Presentation;

namespace SolarMicrogrid.Web.Controllers;

[Authorize(Roles = Roles.GridOperator)]
public class OperatorController : PortalController
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

            PageMeta["PendingCount"] = pending.Count;
            PageMeta["ApprovedFutureCount"] = allBookings.Count(b =>
                b.Status == "APPROVED" && b.ReservationDate > now);
            PageMeta["CompletedCount"] = allBookings.Count(b => b.Status == "COMPLETED");

            PageMeta["PendingReservations"] = pending.Take(5).ToList();
            PageMeta["RecentCompleted"] = allBookings
                .Where(b => b.Status == "COMPLETED")
                .OrderByDescending(b => b.CompletedAt ?? b.UpdatedAt)
                .Take(5)
                .ToList();

            return ReactPage();
        }
        catch (Exception ex)
        {
            PageMeta["Error"] = ex.Message;
            PageMeta["PendingCount"] = 0;
            PageMeta["ApprovedFutureCount"] = 0;
            PageMeta["CompletedCount"] = 0;
            PageMeta["PendingReservations"] = new List<ReservationResponse>();
            PageMeta["RecentCompleted"] = new List<ReservationResponse>();
            return ReactPage();
        }
    }
}
