using MongoDB.Bson.Serialization.Attributes;

namespace SolarMicrogrid.Api.Models;

[BsonIgnoreExtraElements]
public sealed class StaffUser : AccountDocument
{
    public string Username { get; set; } = "";

    public bool IsProtected { get; set; }
}
