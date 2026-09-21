using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Models;

[BsonIgnoreExtraElements]
public abstract record AccountDocument
{
    [BsonId] public string Id { get; init; } = "";
    public string FullName { get; init; } = "";
    public string Email { get; init; } = "";
    public string PasswordHash { get; init; } = "";
    public string Role { get; init; } = "";
    [BsonRepresentation(BsonType.String)] public AccountStatus Status { get; init; } = AccountStatus.Active;
    public long Version { get; init; } = 1;
    public long SecurityVersion { get; init; } = 1;
    public DateTime CreatedAt { get; init; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; init; } = DateTime.UtcNow;
    public List<AccountEvent> RecentEvents { get; init; } = [];
}

[BsonIgnoreExtraElements]
public sealed record StaffUser : AccountDocument
{
    public string Username { get; init; } = "";
    public bool IsProtected { get; init; }
}

[BsonIgnoreExtraElements]
public sealed record Prosumer : AccountDocument
{
    public string Phone { get; init; } = "";
    public string Address { get; init; } = "";
    public DeactivationRequest? DeactivationRequest { get; init; }
}

public sealed record DeactivationRequest
{
    public string Id { get; init; } = Guid.NewGuid().ToString("N");
    public string Reason { get; init; } = "";
    [BsonRepresentation(BsonType.String)] public RequestStatus Status { get; init; } = RequestStatus.Pending;
    public DateTime RequestedAt { get; init; } = DateTime.UtcNow;
    public string? ReviewedBy { get; init; }
    public DateTime? ReviewedAt { get; init; }
    public string? DecisionNote { get; init; }
}

public sealed record AccountEvent(string Action, string ActorId, string? Note, DateTime At);
