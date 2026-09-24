namespace SolarMicrogrid.Contracts;

public sealed record StationSchedule(
    int[] Days,
    string OpensAt,
    string ClosesAt,
    string TimeZone
);
