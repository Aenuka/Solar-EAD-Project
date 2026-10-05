// File: StationResponse.cs
// Purpose: Exposes station details, schedule, and availability to clients.
// Group member responsible: Chamithu Edirimanna (IT23202054).

namespace SolarMicrogrid.Contracts;

public sealed record StationResponse(
    string Id,
    string Name,
    string Address,
    double Latitude,
    double Longitude,
    double CapacityKw,
    double StorageKwh,
    int BatterySlots,
    bool Active,
    long Version,
    StationSchedule Schedule,
    int ActiveReservations,
    IReadOnlyList<EnergySlotResponse> Slots,
    double? DistanceKm = null
);
