/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Defines Backoffice-only endpoints for listing, creating, viewing, and updating staff accounts.
 */

using System.ComponentModel.DataAnnotations;
using System.Security.Claims;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Endpoints;

// Reference: Julio Casal YouTube tutorials
// https://www.youtube.com/@juliocasal
public static class StaffEndpoints
{
    private const string GetStaffEndpointName = "GetStaff";

    public static void MapStaffEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/v1/staff-users")
            .WithTags("StaffUsers")
            .RequireAuthorization(policy => policy.RequireRole(Roles.Backoffice));

        // GET /api/v1/staff-users
        group.MapGet("", (StaffService service, CancellationToken ct,
            [Range(1, 100000)] int page = 1, [Range(1, 100)] int pageSize = 20) =>
            service.ListAsync(page, pageSize, ct));

        // GET /api/v1/staff-users/{id}
        group.MapGet("/{id}", (string id, StaffService service, CancellationToken ct) =>
            service.GetAsync(id, ct))
            .WithName(GetStaffEndpointName);

        // POST /api/v1/staff-users
        group.MapPost("", async (CreateStaffRequest request, ClaimsPrincipal user,
            StaffService service, CancellationToken ct) =>
        {
            var staff = await service.CreateAsync(request, user.FindFirstValue("sub")!, ct);
            return Results.CreatedAtRoute(GetStaffEndpointName, new { id = staff.Id }, staff);
        })
            .WithJsonBody<CreateStaffRequest>()
            .Produces<StaffResponse>(StatusCodes.Status201Created);

        // PATCH /api/v1/staff-users/{id}
        group.MapPatch("/{id}", (string id, UpdateStaffRequest request, ClaimsPrincipal user,
            StaffService service, CancellationToken ct) =>
            service.UpdateAsync(id, request, user.FindFirstValue("sub")!, ct))
            .WithJsonBody<UpdateStaffRequest>();
    }
}
