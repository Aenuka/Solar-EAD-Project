// File: EnergySlot.cs
// Purpose: Stores a station energy window and its booking allocations.
// Group member responsible: Chamithu Edirimanna (IT23202054).

namespace SolarMicrogrid.Api.Models;

public sealed class EnergySlot
{
    public string Id { get; set; } = Guid.NewGuid().ToString("N");

    public DateTime StartsAt { get; set; }

    public DateTime EndsAt { get; set; }

    public int UsableSlots { get; set; }

    public double UsableEnergyKwh { get; set; }

    public List<StationAllocation> Allocations { get; set; } = [];
}
