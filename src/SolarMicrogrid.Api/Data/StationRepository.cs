using MongoDB.Driver;
using SolarMicrogrid.Api.Models;

namespace SolarMicrogrid.Api.Data;

public sealed class StationRepository(IMongoDatabase database)
{
    private readonly IMongoCollection<SolarStation> stations = database.GetCollection<SolarStation>("solarStations");

    public async Task<SolarStation?> FindAsync(string id, CancellationToken ct) =>
        await stations.Find(station => station.Id == id).FirstOrDefaultAsync(ct);

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

    public Task InsertAsync(SolarStation station, CancellationToken ct) =>
        stations.InsertOneAsync(station, cancellationToken: ct);

    public async Task<bool> ReplaceAsync(SolarStation station, long expectedVersion, CancellationToken ct)
    {
        var result = await stations.ReplaceOneAsync(
            existing => existing.Id == station.Id && existing.Version == expectedVersion,
            station, cancellationToken: ct);
        return result.ModifiedCount == 1;
    }
}
