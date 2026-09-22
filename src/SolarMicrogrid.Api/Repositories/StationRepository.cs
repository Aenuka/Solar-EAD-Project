using MongoDB.Driver;
using SolarMicrogrid.Api.Models;

namespace SolarMicrogrid.Api.Repositories;

public interface IStationRepository
{
    Task<SolarStation?> FindAsync(string id, CancellationToken ct);
    Task<List<SolarStation>> ListAsync(bool activeOnly, CancellationToken ct);
    Task InsertAsync(SolarStation station, CancellationToken ct);
    Task<bool> ReplaceAsync(SolarStation station, long version, CancellationToken ct);
}
public sealed class StationRepository(IMongoDatabase db) : IStationRepository
{
    private readonly IMongoCollection<SolarStation> stations = db.GetCollection<SolarStation>("solarStations");
    public async Task<SolarStation?> FindAsync(string id, CancellationToken ct) =>
        await stations.Find(x => x.Id == id).FirstOrDefaultAsync(ct);
    public Task<List<SolarStation>> ListAsync(bool activeOnly, CancellationToken ct) =>
        stations.Find(activeOnly ? Builders<SolarStation>.Filter.Eq(x => x.Active, true) : Builders<SolarStation>.Filter.Empty)
            .SortBy(x => x.Name).ThenBy(x => x.Id).ToListAsync(ct);
    public Task InsertAsync(SolarStation station, CancellationToken ct) => stations.InsertOneAsync(station, cancellationToken: ct);
    public async Task<bool> ReplaceAsync(SolarStation station, long version, CancellationToken ct) =>
        (await stations.ReplaceOneAsync(x => x.Id == station.Id && x.Version == version, station, cancellationToken: ct)).ModifiedCount == 1;
}
