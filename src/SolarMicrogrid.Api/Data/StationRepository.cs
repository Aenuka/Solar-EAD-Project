// File: StationRepository.cs
// Purpose: Reads and writes station documents in MongoDB.
// Group member responsible: Chamithu Edirimanna (IT23202054).

using MongoDB.Driver;
using SolarMicrogrid.Api.Models;

namespace SolarMicrogrid.Api.Data;

public sealed class StationRepository(IMongoDatabase database)
{
    private readonly IMongoCollection<SolarStation> stations = database.GetCollection<SolarStation>("solarStations");

    // Finds a station by its stable identifier.
    public async Task<SolarStation?> FindAsync(string id, CancellationToken ct) =>
        await stations.Find(station => station.Id == id).FirstOrDefaultAsync(ct);

    // Loads the stations matching a collection of identifiers.
    public Task<List<SolarStation>> FindManyAsync(IEnumerable<string> ids, CancellationToken ct) =>
        stations.Find(Builders<SolarStation>.Filter.In(station => station.Id, ids)).ToListAsync(ct);

    // Lists stations in a stable order with optional active filtering.
    public Task<List<SolarStation>> ListAsync(bool activeOnly, CancellationToken ct)
    {
        var filter = Builders<SolarStation>.Filter.Empty;
        if (activeOnly)
        {
            filter = Builders<SolarStation>.Filter.Eq(station => station.Active, true);
        }

        return stations.Find(filter)
            .SortBy(station => station.Name)
            .ThenBy(station => station.Id)
            .ToListAsync(ct);
    }

    // Inserts a newly registered station.
    public Task InsertAsync(SolarStation station, CancellationToken ct) =>
        stations.InsertOneAsync(station, cancellationToken: ct);

    // Replaces a station only when its stored version matches the caller's version.
    public async Task<bool> ReplaceAsync(SolarStation station, long expectedVersion, CancellationToken ct)
    {
        var result = await stations.ReplaceOneAsync(
            existing => existing.Id == station.Id && existing.Version == expectedVersion,
            station, cancellationToken: ct);
        return result.ModifiedCount == 1;
    }
}
