using System.ComponentModel.DataAnnotations;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Web.ViewModels;

public sealed class StationSlotForm : StationVersion
{
    [Required] public DateTime? StartsAt { get; set; }
    [Required] public DateTime? EndsAt { get; set; }
    [Required, Range(0, 10000)] public int? UsableSlots { get; set; }
    [Required, Range(0, 1000000)] public double? UsableEnergyKwh { get; set; }
    // Convert presentation-local times to unambiguous API timestamps; rules stay in StationService.
    public SlotInput ToRequest() => new()
    {
        Version = Version,
        StartsAt = new DateTimeOffset(DateTime.SpecifyKind(StartsAt!.Value, DateTimeKind.Unspecified), TimeSpan.FromMinutes(330)),
        EndsAt = new DateTimeOffset(DateTime.SpecifyKind(EndsAt!.Value, DateTimeKind.Unspecified), TimeSpan.FromMinutes(330)),
        UsableSlots = UsableSlots!.Value, UsableEnergyKwh = UsableEnergyKwh!.Value
    };
}
