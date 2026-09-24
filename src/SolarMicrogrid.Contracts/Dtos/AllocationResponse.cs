namespace SolarMicrogrid.Contracts;

public sealed record AllocationResponse(
    string BookingId,
    int Slots,
    double EnergyKwh,
    string Status
);
