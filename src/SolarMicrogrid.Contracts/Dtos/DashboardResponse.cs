namespace SolarMicrogrid.Contracts;

public sealed record DashboardResponse(
    long ActiveProsumers,
    long InactiveProsumers,
    long PendingRequests,
    long StaffUsers
);
