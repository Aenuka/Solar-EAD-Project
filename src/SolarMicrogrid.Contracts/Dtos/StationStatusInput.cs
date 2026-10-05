// File: StationStatusInput.cs
// Purpose: Carries an activation change and its station version.
// Group member responsible: Chamithu Edirimanna (IT23202054).

using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public sealed class StationStatusInput : StationVersion
{
    [JsonRequired]
    public bool Active { get; set; }
}
