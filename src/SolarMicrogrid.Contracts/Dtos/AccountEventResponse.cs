namespace SolarMicrogrid.Contracts;

public sealed record AccountEventResponse(
    string Action,
    string ActorId,
    string? Note,
    DateTimeOffset At
);
