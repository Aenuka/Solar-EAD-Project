// File: AllocationInput.cs
// Purpose: Validates booking capacity allocated from a station window.
// Group member responsible: Chamithu Edirimanna (IT23202054).

using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Contracts;

public sealed class AllocationInput : StationVersion
{
    [Required, RegularExpression("^[a-zA-Z0-9_-]{1,80}$")]
    public string BookingId { get; set; } = "";

    [Range(1, 10000)]
    public int Slots { get; set; }

    [Range(0.01, 1000000)]
    public double EnergyKwh { get; set; }
}
