using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public sealed class SlotInput : StationVersion
{
    [JsonRequired]
    public DateTimeOffset StartsAt { get; set; }

    [JsonRequired]
    public DateTimeOffset EndsAt { get; set; }

    [JsonRequired, Range(0, 10000)]
    public int UsableSlots { get; set; }

    [JsonRequired, Range(0, 1000000)]
    public double UsableEnergyKwh { get; set; }
}
