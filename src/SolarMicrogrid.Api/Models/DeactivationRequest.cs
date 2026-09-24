using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Models;

public sealed class DeactivationRequest
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");

    public string Reason { get; set; } = "";

    [BsonRepresentation(BsonType.String)]
    public RequestStatus Status { get; set; } = RequestStatus.Pending;

    public DateTime RequestedAt { get; set; } = DateTime.UtcNow;

    public string? ReviewedBy { get; set; }

    public DateTime? ReviewedAt { get; set; }

    public string? DecisionNote { get; set; }
}
