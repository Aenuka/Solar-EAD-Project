using System.ComponentModel.DataAnnotations;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Controllers;

[ApiController, Authorize(Roles = Roles.Backoffice), Route("api/v1/staff-users")]
public sealed class StaffUsersController(StaffService service) : ControllerBase
{
    [HttpGet]
    public Task<PageResponse<StaffResponse>> List([Range(1, 100000)] int page = 1, [Range(1, 100)] int pageSize = 20,
        CancellationToken ct = default) => service.ListAsync(page, pageSize, ct);

    [HttpGet("{id}")]
    public Task<StaffResponse> Get(string id, CancellationToken ct) => service.GetAsync(id, ct);

    [HttpPost, ProducesResponseType<StaffResponse>(201)]
    public async Task<ActionResult<StaffResponse>> Create(CreateStaffRequest request, CancellationToken ct)
    {
        var result = await service.CreateAsync(request, User.FindFirstValue("sub")!, ct);
        return CreatedAtAction(nameof(Get), new { id = result.Id }, result);
    }

    [HttpPatch("{id}")]
    public Task<StaffResponse> Update(string id, UpdateStaffRequest request, CancellationToken ct) =>
        service.UpdateAsync(id, request, User.FindFirstValue("sub")!, ct);
}
