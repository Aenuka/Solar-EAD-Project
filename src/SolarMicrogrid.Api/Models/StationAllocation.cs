namespace SolarMicrogrid.Api.Models;

public sealed record StationAllocation(string BookingId, int Slots, double EnergyKwh, string Status);
