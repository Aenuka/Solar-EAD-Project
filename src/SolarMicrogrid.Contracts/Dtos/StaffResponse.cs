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
