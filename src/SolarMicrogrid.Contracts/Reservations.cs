/*
 * File: Reservations.cs
 * Author: Sajith
 * Description: DTOs for reservation operations.
 */

namespace SolarMicrogrid.Contracts;

public class ReservationInput
{
    public string ProsumerNic { get; set; }
    public string StationId { get; set; }
    public string SlotId { get; set; }
    public DateTime ReservationDate { get; set; }
    public double EnergyAmountKwh { get; set; }
    public string TradingType { get; set; }
}

public class ReservationResponse
{
    public string Id { get; set; }
    public string ReservationId { get; set; }
    public string ProsumerNic { get; set; }
    public string StationId { get; set; }
    public string SlotId { get; set; }
    public DateTime ReservationDate { get; set; }
    public string Status { get; set; }
    public DateTime CreatedAt { get; set; }
}