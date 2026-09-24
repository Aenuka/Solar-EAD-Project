namespace SolarMicrogrid.Contracts;

public sealed record AuthResponse(
    string AccessToken,
    DateTimeOffset ExpiresAt,
    string TokenType,
    string Id,
    string FullName,
    string Role
);
