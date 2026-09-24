namespace SolarMicrogrid.Contracts;

public sealed record PageResponse<T>(
    IReadOnlyList<T> Items,
    long Total,
    int Page,
    int PageSize
);
