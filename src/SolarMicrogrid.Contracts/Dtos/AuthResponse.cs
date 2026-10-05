/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Returns the JWT access token, expiry, token type, account identity, and role after login.
 */

namespace SolarMicrogrid.Contracts;

public sealed record AuthResponse(
    string AccessToken,
    DateTimeOffset ExpiresAt,
    string TokenType,
    string Id,
    string FullName,
    string Role
);
