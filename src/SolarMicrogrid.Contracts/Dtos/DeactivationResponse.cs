/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Returns account deactivation request details, review status, reviewer, and decision note.
 */

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
