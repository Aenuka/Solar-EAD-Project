// File: StationSchedule.cs
// Purpose: Describes a station's operating days, hours, and time zone.
// Group member responsible: Chamithu Edirimanna (IT23202054).

namespace SolarMicrogrid.Contracts;

public sealed record StationSchedule(
    int[] Days,
    string OpensAt,
    string ClosesAt,
    string TimeZone
);
