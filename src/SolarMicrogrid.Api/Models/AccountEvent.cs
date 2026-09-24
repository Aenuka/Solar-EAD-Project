namespace SolarMicrogrid.Api.Models;

public sealed record AccountEvent(string Action, string ActorId, string? Note, DateTime At);
