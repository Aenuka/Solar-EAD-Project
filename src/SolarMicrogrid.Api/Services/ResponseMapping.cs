using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public static class ResponseMapping
{
    public static StaffResponse ToResponse(this StaffUser user) =>
        new(user.Id, user.Username, user.FullName, user.Email, user.Role,
            user.Status, user.IsProtected, user.Version, Timestamp(user.CreatedAt));

    public static ProsumerResponse ToResponse(this Prosumer user)
    {
        DeactivationResponse? deactivation = null;
        if (user.DeactivationRequest is not null)
        {
            var request = user.DeactivationRequest;
            deactivation = new DeactivationResponse(request.Id, request.Reason, request.Status,
                Timestamp(request.RequestedAt), request.ReviewedBy,
                request.ReviewedAt.HasValue ? Timestamp(request.ReviewedAt.Value) : null,
                request.DecisionNote);
        }

        var events = user.RecentEvents.Select(accountEvent =>
            new AccountEventResponse(accountEvent.Action, accountEvent.ActorId, accountEvent.Note, Timestamp(accountEvent.At)))
            .ToList();

        return new ProsumerResponse(user.Id, user.FullName, user.Email, user.Phone, user.Address,
            user.Status, user.Version, Timestamp(user.CreatedAt), deactivation, events);
    }

    // BSON dates have millisecond precision. Newly written and subsequently read responses must agree.
    private static DateTimeOffset Timestamp(DateTime value) =>
        DateTimeOffset.FromUnixTimeMilliseconds(new DateTimeOffset(value).ToUnixTimeMilliseconds());

    public static List<AccountEvent> WithEvent(this AccountDocument user, string action, string actor, string? note = null) =>
        user.RecentEvents.Append(new AccountEvent(action, actor, note, DateTime.UtcNow)).TakeLast(100).ToList();

    public static void CheckVersion(long actual, long expected)
    {
        if (actual != expected)
        {
            throw ApiException.Conflict("This account changed. Reload it before trying again.");
        }
    }
}
