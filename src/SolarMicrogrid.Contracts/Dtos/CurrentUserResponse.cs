/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Returns the authenticated account's identifier, full name, and role.
 */

namespace SolarMicrogrid.Contracts;

public sealed record CurrentUserResponse(
    string Id,
    string FullName,
    string Role
);
