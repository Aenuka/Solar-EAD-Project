/*
 * File: ReservationsController.cs
 * Author: Sajith
 * Description: REST API for reservations.
 */

using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ReservationsController : ControllerBase
{
    private readonly ReservationService _service;

    public ReservationsController(ReservationService service)
    {
        _service = service;
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] ReservationInput dto)
    {
        var (success, message, reservation) = await _service.CreateAsync(dto);

        if (!success)
            return BadRequest(new { success = false, message });

        return Ok(new { success = true, data = reservation, message });
    }
}