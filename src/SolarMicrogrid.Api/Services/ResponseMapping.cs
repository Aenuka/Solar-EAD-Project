using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public static class ResponseMapping
{
    public static StaffResponse ToResponse(this StaffUser user) => new(user.Id, user.Username, user.FullName,
        user.Email, user.Role, user.Status, user.IsProtected, user.Version, Timestamp(user.CreatedAt));

    public static ProsumerResponse ToResponse(this Prosumer user) => new(user.Id, user.FullName, user.Email,
        user.Phone, user.Address, user.Status, user.Version, Timestamp(user.CreatedAt),
        user.DeactivationRequest is { } r ? new(r.Id, r.Reason, r.Status, Timestamp(r.RequestedAt), r.ReviewedBy,
            r.ReviewedAt is { } reviewed ? Timestamp(reviewed) : null, r.DecisionNote) : null,
        user.RecentEvents.Select(e => new AccountEventResponse(e.Action, e.ActorId, e.Note, Timestamp(e.At))).ToList());

    // BSON dates have millisecond precision. Newly written and subsequently read responses must agree.
    private static DateTimeOffset Timestamp(DateTime value) =>
        DateTimeOffset.FromUnixTimeMilliseconds(new DateTimeOffset(value).ToUnixTimeMilliseconds());

    public static List<AccountEvent> WithEvent(this AccountDocument user, string action, string actor, string? note = null) =>
        user.RecentEvents.Append(new AccountEvent(action, actor, note, DateTime.UtcNow)).TakeLast(100).ToList();

    public static void CheckVersion(long actual, long expected)
    {
        if (actual != expected) throw ApiException.Conflict("This account changed. Reload it before trying again.");
    }
}
