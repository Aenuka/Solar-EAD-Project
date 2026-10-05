// File: SolarStation.cs
// Purpose: Stores station details, operating schedule, and energy windows.
// Group member responsible: Chamithu Edirimanna (IT23202054).

using MongoDB.Bson.Serialization.Attributes;

namespace SolarMicrogrid.Api.Models;

// Stores station capacity, operating hours, availability windows, and the concurrency version. *****
public sealed class SolarStation
{
    [BsonId]
    public string Id { get; set; } = Guid.NewGuid().ToString("N");

    public string Name { get; set; } = "";

    public string Address { get; set; } = "";

    public double Latitude { get; set; }

    public double Longitude { get; set; }

    public double CapacityKw { get; set; }

    public double StorageKwh { get; set; }

    public int BatterySlots { get; set; }

    public bool Active { get; set; } = true;

    public long Version { get; set; } = 1;

    public int[] Days { get; set; } = [0, 1, 2, 3, 4, 5, 6];

    public string OpensAt { get; set; } = "08:00";

    public string ClosesAt { get; set; } = "18:00";

    public List<EnergySlot> Slots { get; set; } = [];
}
