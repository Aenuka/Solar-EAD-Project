using MongoDB.Bson.Serialization.Attributes;

namespace SolarMicrogrid.Api.Models;

[BsonIgnoreExtraElements]
public sealed class Prosumer : AccountDocument
{
    public string Phone { get; set; } = "";

    public string Address { get; set; } = "";

    public DeactivationRequest? DeactivationRequest { get; set; }
}
