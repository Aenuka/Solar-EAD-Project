namespace SolarMicrogrid.Contracts;

public sealed record CurrentUserResponse(
    string Id,
    string FullName,
    string Role
);
