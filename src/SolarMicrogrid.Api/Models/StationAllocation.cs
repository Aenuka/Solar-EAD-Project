// File: StationAllocation.cs
// Purpose: Records the capacity reserved or consumed by a booking.
// Group member responsible: Chamithu Edirimanna (IT23202054).

namespace SolarMicrogrid.Api.Models;

public sealed record StationAllocation(string BookingId, int Slots, double EnergyKwh, string Status);
