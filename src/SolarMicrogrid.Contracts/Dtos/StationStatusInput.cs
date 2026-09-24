using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public sealed class StationStatusInput : StationVersion
{
    [JsonRequired]
    public bool Active { get; set; }
}
