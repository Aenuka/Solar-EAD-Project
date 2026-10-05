// File: StationVersion.cs
// Purpose: Carries the version required for safe station updates.
// Group member responsible: Chamithu Edirimanna (IT23202054).

using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public class StationVersion
{
    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}
