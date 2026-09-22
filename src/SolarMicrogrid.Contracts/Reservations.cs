/*
 * File: Reservations.cs
 * Author: Sajith
 * Description: DTOs for reservation operations.
 */

using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public class ReservationInput
{
    [Required, StringLength(20, MinimumLength = 10)]
    public string ProsumerNic { get; set; } = string.Empty;

    [Required]
    public string StationId { get; set; } = string.Empty;

    [Required]
    public string SlotId { get; set; } = string.Empty;

    [JsonRequired]
    public DateTime ReservationDate { get; set; }

    [Range(0.01, 1000000)]
    public double EnergyAmountKwh { get; set; }

    [Required, StringLength(50)]
    public string TradingType { get; set; } = string.Empty;
}

public class ReservationResponse
{
    [Required]
    public string Id { get; set; } = string.Empty;

    [Required]
    public string ReservationId { get; set; } = string.Empty;

    [Required]
    public string ProsumerNic { get; set; } = string.Empty;

    [Required]
    public string StationId { get; set; } = string.Empty;

    [Required]
    public string SlotId { get; set; } = string.Empty;

    [JsonRequired]
    public DateTime ReservationDate { get; set; }

    [Required]
    public string Status { get; set; } = string.Empty;

    [JsonRequired]
    public DateTime CreatedAt { get; set; }
}