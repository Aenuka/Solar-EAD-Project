/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Returns an account audit event's action, actor, note, and timestamp.
 */

namespace SolarMicrogrid.Contracts;

public sealed record AccountEventResponse(
    string Action,
    string ActorId,
    string? Note,
    DateTimeOffset At
);
