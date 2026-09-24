namespace SolarMicrogrid.Contracts;

public sealed record DeactivationResponse(
    string Id,
    string Reason,
    RequestStatus Status,
    DateTimeOffset RequestedAt,
    string? ReviewedBy,
    DateTimeOffset? ReviewedAt,
    string? DecisionNote
);
