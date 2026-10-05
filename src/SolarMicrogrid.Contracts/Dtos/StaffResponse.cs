/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Returns staff identity, role, status, protection flag, version, and creation time.
 */

namespace SolarMicrogrid.Contracts;

public sealed record StaffResponse(
    string Id,
    string Username,
    string FullName,
    string Email,
    string Role,
    AccountStatus Status,
    bool IsProtected,
    long Version,
    DateTimeOffset CreatedAt
);
