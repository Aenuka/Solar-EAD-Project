/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Represents an account audit event with its action, actor, note, and timestamp.
 */

namespace SolarMicrogrid.Api.Models;

public sealed record AccountEvent(string Action, string ActorId, string? Note, DateTime At);
