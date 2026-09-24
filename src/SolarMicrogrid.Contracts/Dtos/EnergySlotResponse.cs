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
