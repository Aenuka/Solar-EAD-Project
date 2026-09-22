/*
 * File: EnergyReservation.cs
 * Author: Sajith
 * Description: MongoDB model for energy reservations.
 */

using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SolarMicrogrid.Api.Models;

public class EnergyReservation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; }
    public string ReservationId { get; set; }
    public string ProsumerNic { get; set; }
    public string StationId { get; set; }
    public string SlotId { get; set; }
    public DateTime ReservationDate { get; set; }
    public double EnergyAmountKwh { get; set; }
    public string TradingType { get; set; }
    public string Status { get; set; } // PENDING, APPROVED, CANCELLED, COMPLETED
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}