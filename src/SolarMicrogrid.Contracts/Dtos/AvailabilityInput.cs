// File: AvailabilityInput.cs
// Purpose: Validates updated usable station window capacity.
// Group member responsible: Chamithu Edirimanna (IT23202054).

using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public sealed class AvailabilityInput : StationVersion
{
    [JsonRequired, Range(0, 10000)]
    public int UsableSlots { get; set; }

    [JsonRequired, Range(0, 1000000)]
    public double UsableEnergyKwh { get; set; }
}
