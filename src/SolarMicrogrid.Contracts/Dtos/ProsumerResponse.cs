namespace SolarMicrogrid.Contracts;

// Reference: Julio Casal YouTube tutorials
// https://www.youtube.com/@juliocasal
public sealed record ProsumerResponse(
    string Nic,
    string FullName,
    string Email,
    string Phone,
    string Address,
    AccountStatus Status,
    long Version,
    DateTimeOffset CreatedAt,
    DeactivationResponse? DeactivationRequest,
    IReadOnlyList<AccountEventResponse> RecentEvents
);
