/*
 * File: ReservationsController.cs
 * Author: Sajith
 * Description: REST API endpoints for reservation management.
 */

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Controllers;

[ApiController]
[Route("api/v1/[controller]")]
public class ReservationsController : ControllerBase
{
    private readonly ReservationService _service;

    public ReservationsController(ReservationService service)
    {
        _service = service;
    }

    private static ReservationResponse Map(EnergyReservation r) => new()
    {
        Id = r.Id ?? string.Empty,
        ReservationId = r.ReservationId,
        ProsumerNic = r.ProsumerNic,
        StationId = r.StationId,
        SlotId = r.SlotId,
        ReservationDate = r.ReservationDate,
        EnergyAmountKwh = r.EnergyAmountKwh,
        TradingType = r.TradingType,
        Status = r.Status,
        CreatedAt = r.CreatedAt,
        UpdatedAt = r.UpdatedAt,
        TransactionToken = r.TransactionToken,
        CancellationReason = r.CancellationReason,
        CompletedAt = r.CompletedAt,
        StationVersion = r.StationVersion,
        AllocationSlots = r.AllocationSlots
    };

    // ===== CREATE =====
    [HttpPost]
    public async Task<IActionResult> Create([FromBody] ReservationInput dto)
    {
        var (success, message, reservation) = await _service.CreateAsync(dto);
        if (!success || reservation is null)
            return Problem(detail: message, statusCode: 400);
        return Ok(Map(reservation));
    }

    // ===== UPDATE =====
    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateReservationInput dto)
    {
        var (success, message, reservation) = await _service.UpdateAsync(id, dto);
        if (!success || reservation is null)
            return Problem(detail: message, statusCode: 400);
        return Ok(Map(reservation));
    }

    // ===== CANCEL =====
    [HttpPatch("{id}/cancel")]
    public async Task<IActionResult> Cancel(string id, [FromBody] CancelReservationInput dto)
    {
        var (success, message, reservation) = await _service.CancelAsync(id, dto);
        if (!success || reservation is null)
            return Problem(detail: message, statusCode: 400);
        return Ok(Map(reservation));
    }

    // ===== HISTORY =====
    [HttpGet("history")]
    public async Task<IActionResult> GetHistory([FromQuery] string nic)
    {
        if (string.IsNullOrEmpty(nic))
            return Problem(detail: "NIC is required.", statusCode: 400);
        var list = await _service.GetHistoryAsync(nic);
        return Ok(list.Select(Map));
    }

    // ===== PENDING =====
    [HttpGet("pending")]
    public async Task<IActionResult> GetPending()
    {
        var list = await _service.GetPendingAsync();
        return Ok(list.Select(Map));
    }

    // ===== SEARCH =====
    [HttpGet("search")]
    public async Task<IActionResult> Search(
        [FromQuery] string? status,
        [FromQuery] string? stationId,
        [FromQuery] string? nic,
        [FromQuery] DateTime? from,
        [FromQuery] DateTime? to)
    {
        var list = await _service.SearchAsync(status, stationId, nic, from, to);
        return Ok(list.Select(Map));
    }

    // ===== APPROVED FUTURE COUNT =====
    [HttpGet("approved-future/count")]
    public async Task<IActionResult> GetApprovedFutureCount()
    {
        var count = await _service.GetApprovedFutureCountAsync();
        return Ok(new { count });
    }

    // ===== DASHBOARD (Operator only) =====
    /// <summary>
    /// Retrieves live dashboard data for Grid Operators.
    /// </summary>
    [Authorize(Roles = "GridOperator")]
    [HttpGet("dashboard")]
    public async Task<IActionResult> GetDashboard()
    {
        var (pending, approvedFutureCount, completed) = await _service.GetDashboardDataAsync();
        
        var response = new OperatorDashboardResponse
        {
            PendingCount = pending.Count,
            ApprovedFutureCount = approvedFutureCount,
            CompletedCount = completed.Count,
            PendingReservations = pending.Select(Map).ToList(),
            RecentCompletedReservations = completed.OrderByDescending(c => c.CompletedAt).Take(5).Select(Map).ToList()
        };
        
        return Ok(response);
    }

    // ===== TRANSACTION (QR) =====
    [Authorize(Roles = "Prosumer")]
    [HttpGet("{id}/transaction")]
    public async Task<IActionResult> GetTransaction(string id)
    {
        var nic = User.Claims.FirstOrDefault(c => c.Type == "sub")?.Value;
        if (string.IsNullOrEmpty(nic))
            return Unauthorized();

        var (success, message, reservation) = await _service.GetTransactionAsync(id, nic);
        if (!success || reservation is null)
        {
            if (message == "Unauthorized.")
                return Forbid();
            return Problem(detail: message, statusCode: 400);
        }
        return Ok(Map(reservation));
    }

    // ===== APPROVE (Operator only) =====
    /// <summary>
    /// Approves a pending reservation.
    /// </summary>
    [Authorize(Roles = "Backoffice,GridOperator")]
    [HttpPatch("{id}/approve")]
    public async Task<IActionResult> Approve(string id)
    {
        var (success, message, reservation) = await _service.ApproveAsync(id);
        if (!success || reservation is null)
            return Problem(detail: message, statusCode: 400);
        return Ok(Map(reservation));
    }

    // ===== VERIFY TOKEN (Operator only) =====
    /// <summary>
    /// Verifies a scanned transaction token.
    /// </summary>
    [Authorize(Roles = "GridOperator")]
    [HttpGet("verify")]
    public async Task<IActionResult> VerifyToken([FromQuery] string token)
    {
        var (success, message, reservation) = await _service.VerifyTokenAsync(token);
        if (!success || reservation is null)
            return Problem(detail: message, statusCode: 400);
        return Ok(Map(reservation));
    }

    // ===== COMPLETE (Operator only) =====
    /// <summary>
    /// Finalizes the energy transfer for an approved reservation.
    /// </summary>
    [Authorize(Roles = "GridOperator")]
    [HttpPatch("{id}/complete")]
    public async Task<IActionResult> Complete(string id)
    {
        var (success, message, reservation) = await _service.CompleteAsync(id);
        if (!success || reservation is null)
            return Problem(detail: message, statusCode: 400);
        return Ok(Map(reservation));
    }
}