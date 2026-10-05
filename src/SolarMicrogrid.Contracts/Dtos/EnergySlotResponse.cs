// File: EnergySlotResponse.cs
// Purpose: Reports an energy window's capacity and remaining availability.
// Group member responsible: Chamithu Edirimanna (IT23202054).

namespace SolarMicrogrid.Contracts;

public sealed record EnergySlotResponse(
    string Id,
    DateTimeOffset StartsAt,
    DateTimeOffset EndsAt,
    int UsableSlots,
    double UsableEnergyKwh,
    int AvailableSlots,
    double AvailableEnergyKwh,
    int ActiveReservations,
    IReadOnlyList<AllocationResponse> Allocations
);
