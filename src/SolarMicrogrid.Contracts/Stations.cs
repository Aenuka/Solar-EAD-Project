using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public class StationInput
{
    [Required, StringLength(100, MinimumLength = 2)] public string Name { get; set; } = "";
    [Required, StringLength(300, MinimumLength = 5)] public string Address { get; set; } = "";
    [JsonRequired, Range(-90d, 90d)] public double Latitude { get; set; }
    [JsonRequired, Range(-180d, 180d)] public double Longitude { get; set; }
    [Range(0.01, 1000000)] public double CapacityKw { get; set; }
    [Range(0.01, 1000000)] public double StorageKwh { get; set; }
    [Range(1, 10000)] public int BatterySlots { get; set; }
}
public sealed class StationUpdate : StationInput
{
    [JsonRequired, Range(1, long.MaxValue)] public long Version { get; set; }
}
public class StationVersion
{
    [JsonRequired, Range(1, long.MaxValue)] public long Version { get; set; }
}
public sealed class StationStatusInput : StationVersion
{
    [JsonRequired] public bool Active { get; set; }
}
public sealed class ScheduleInput : StationVersion
{
    [Required, MinLength(1), MaxLength(7)] public int[] Days { get; set; } = [];
    [Required, RegularExpression("^([01][0-9]|2[0-3]):[0-5][0-9]$")] public string OpensAt { get; set; } = "08:00";
    [Required, RegularExpression("^([01][0-9]|2[0-3]):[0-5][0-9]$")] public string ClosesAt { get; set; } = "18:00";
}
public sealed class SlotInput : StationVersion
{
    [JsonRequired] public DateTimeOffset StartsAt { get; set; }
    [JsonRequired] public DateTimeOffset EndsAt { get; set; }
    [JsonRequired, Range(0, 10000)] public int UsableSlots { get; set; }
    [JsonRequired, Range(0, 1000000)] public double UsableEnergyKwh { get; set; }
}
public sealed class AvailabilityInput : StationVersion
{
    [JsonRequired, Range(0, 10000)] public int UsableSlots { get; set; }
    [JsonRequired, Range(0, 1000000)] public double UsableEnergyKwh { get; set; }
}
public sealed class AllocationInput : StationVersion
{
    [Required, RegularExpression("^[a-zA-Z0-9_-]{1,80}$")] public string BookingId { get; set; } = "";
    [Range(1, 10000)] public int Slots { get; set; }
    [Range(0.01, 1000000)] public double EnergyKwh { get; set; }
}
public sealed record StationSchedule(int[] Days, string OpensAt, string ClosesAt, string TimeZone);
public sealed record AllocationResponse(string BookingId, int Slots, double EnergyKwh, string Status);
public sealed record EnergySlotResponse(string Id, DateTimeOffset StartsAt, DateTimeOffset EndsAt,
    int UsableSlots, double UsableEnergyKwh, int AvailableSlots, double AvailableEnergyKwh,
    int ActiveReservations, IReadOnlyList<AllocationResponse> Allocations);
public sealed record StationResponse(string Id, string Name, string Address, double Latitude, double Longitude,
    double CapacityKw, double StorageKwh, int BatterySlots, bool Active, long Version,
    StationSchedule Schedule, int ActiveReservations, IReadOnlyList<EnergySlotResponse> Slots, double? DistanceKm = null);
