// File: StationInput.cs
// Purpose: Validates physical station details submitted for registration.
// Group member responsible: Chamithu Edirimanna (IT23202054).

using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public class StationInput
{
    [Required, StringLength(100, MinimumLength = 2)]
    public string Name { get; set; } = "";

    [Required, StringLength(300, MinimumLength = 5)]
    public string Address { get; set; } = "";

    [JsonRequired, Range(-90d, 90d)]
    public double Latitude { get; set; }

    [JsonRequired, Range(-180d, 180d)]
    public double Longitude { get; set; }

    [Range(0.01, 1000000)]
    public double CapacityKw { get; set; }

    [Range(0.01, 1000000)]
    public double StorageKwh { get; set; }

    [Range(1, 10000)]
    public int BatterySlots { get; set; }
}
