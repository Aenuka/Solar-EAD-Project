/*
 * File: ReservationsController.cs
 * Author: Sajith
 * Description: REST API endpoints for reservation management.
 */

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
        CompletedAt = r.CompletedAt
    };

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] ReservationInput dto)
    {
        var (success, message, reservation) = await _service.CreateAsync(dto);
        if (!success || reservation is null)
            return Problem(detail: message, statusCode: 400);
        return Ok(Map(reservation));
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateReservationInput dto)
    {
        var (success, message, reservation) = await _service.UpdateAsync(id, dto);
        if (!success || reservation is null)
            return Problem(detail: message, statusCode: 400);
        return Ok(Map(reservation));
    }

    [HttpPatch("{id}/cancel")]
    public async Task<IActionResult> Cancel(string id, [FromBody] CancelReservationInput dto)
    {
        var (success, message, reservation) = await _service.CancelAsync(id, dto);
        if (!success || reservation is null)
            return Problem(detail: message, statusCode: 400);
        return Ok(Map(reservation));
    }

    [HttpGet("history")]
    public async Task<IActionResult> GetHistory([FromQuery] string nic)
    {
        if (string.IsNullOrEmpty(nic))
            return Problem(detail: "NIC is required.", statusCode: 400);
        var list = await _service.GetHistoryAsync(nic);
        return Ok(list.Select(Map));
    }

    [HttpGet("pending")]
    public async Task<IActionResult> GetPending()
    {
        var list = await _service.GetPendingAsync();
        return Ok(list.Select(Map));
    }

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

    [HttpGet("approved-future/count")]
    public async Task<IActionResult> GetApprovedFutureCount()
    {
        var count = await _service.GetApprovedFutureCountAsync();
        return Ok(new { count });
    }

    [HttpGet("{id}/transaction")]
    public async Task<IActionResult> GetTransaction(string id)
    {
        var (success, message, reservation) = await _service.GetTransactionAsync(id);
        if (!success || reservation is null)
            return Problem(detail: message, statusCode: 400);
        return Ok(Map(reservation));
    }
}