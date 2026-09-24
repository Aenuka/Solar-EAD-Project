using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public sealed class StationUpdate : StationInput
{
    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}
