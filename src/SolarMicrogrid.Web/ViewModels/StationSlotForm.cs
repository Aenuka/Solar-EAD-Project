using System.ComponentModel.DataAnnotations;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Web.ViewModels;

public sealed class StationSlotForm : StationVersion
{
    [Required]
    public DateTime? StartsAt { get; set; }

    [Required]
    public DateTime? EndsAt { get; set; }

    [Required, Range(0, 10000)]
    public int? UsableSlots { get; set; }

    [Required, Range(0, 1000000)]
    public double? UsableEnergyKwh { get; set; }

    public SlotInput ToRequest()
    {
        // The form contains Sri Lanka local times; the API needs timestamps with an offset.
        var colomboOffset = TimeSpan.FromMinutes(330);
        var startsAt = DateTime.SpecifyKind(StartsAt!.Value, DateTimeKind.Unspecified);
        var endsAt = DateTime.SpecifyKind(EndsAt!.Value, DateTimeKind.Unspecified);

        return new SlotInput
        {
            Version = Version,
            StartsAt = new DateTimeOffset(startsAt, colomboOffset),
            EndsAt = new DateTimeOffset(endsAt, colomboOffset),
            UsableSlots = UsableSlots!.Value,
            UsableEnergyKwh = UsableEnergyKwh!.Value
        };
    }
}
