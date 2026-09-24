using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Models;

// Reference: Julio Casal YouTube tutorials
// https://www.youtube.com/@juliocasal
[BsonIgnoreExtraElements]
public abstract class AccountDocument
{
    [BsonId]
    public string Id { get; set; } = "";

    public string FullName { get; set; } = "";

    public string Email { get; set; } = "";

    public string PasswordHash { get; set; } = "";

    public string Role { get; set; } = "";

    [BsonRepresentation(BsonType.String)]
    public AccountStatus Status { get; set; } = AccountStatus.Active;

    public long Version { get; set; } = 1;

    public long SecurityVersion { get; set; } = 1;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public List<AccountEvent> RecentEvents { get; set; } = [];
}
