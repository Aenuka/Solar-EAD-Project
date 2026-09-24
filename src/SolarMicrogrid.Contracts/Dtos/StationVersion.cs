using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public class StationVersion
{
    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}
