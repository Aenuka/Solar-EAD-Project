using MongoDB.Bson.Serialization.Attributes;

namespace SolarMicrogrid.Api.Models;

public sealed record SolarStation
{
    [BsonId] public string Id { get; init; } = Guid.NewGuid().ToString("N");
    public string Name { get; init; } = "";
    public string Address { get; init; } = "";
    public double Latitude { get; init; }
    public double Longitude { get; init; }
    public double CapacityKw { get; init; }
    public double StorageKwh { get; init; }
    public int BatterySlots { get; init; }
    public bool Active { get; init; } = true;
    public long Version { get; init; } = 1;
    public int[] Days { get; init; } = [0, 1, 2, 3, 4, 5, 6];
    public string OpensAt { get; init; } = "08:00";
    public string ClosesAt { get; init; } = "18:00";
    public List<EnergySlot> Slots { get; init; } = [];
}
public sealed record EnergySlot
{
    public string Id { get; init; } = Guid.NewGuid().ToString("N");
    public DateTime StartsAt { get; init; }
    public DateTime EndsAt { get; init; }
    public int UsableSlots { get; init; }
    public double UsableEnergyKwh { get; init; }
    public List<StationAllocation> Allocations { get; init; } = [];
}
public sealed record StationAllocation(string BookingId, int Slots, double EnergyKwh, string Status);
