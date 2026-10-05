/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Persists and queries prosumer accounts in MongoDB, filters account listings, and revokes sessions.
 */

using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Driver;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Data;

public sealed class ProsumerRepository(IMongoDatabase database)
{
    private readonly IMongoCollection<Prosumer> users = database.GetCollection<Prosumer>("prosumers");

    public async Task<Prosumer?> FindAsync(string nic, CancellationToken ct) =>
        await users.Find(user => user.Id == nic).FirstOrDefaultAsync(ct);

    public Task<List<Prosumer>> FindManyAsync(IEnumerable<string> nics, CancellationToken ct) =>
        users.Find(Builders<Prosumer>.Filter.In(user => user.Id, nics)).ToListAsync(ct);

    private static FilterDefinition<Prosumer> CreateFilter(AccountStatus? status, RequestStatus? requestStatus, string? search = null)
    {
        var builder = Builders<Prosumer>.Filter;
        var filter = builder.Empty;
        if (status.HasValue)
        {
            filter &= builder.Eq(user => user.Status, status.Value);
        }
        if (requestStatus.HasValue)
        {
            filter &= builder.Eq("deactivationRequest.status", requestStatus.Value.ToString());
        }
        if (!string.IsNullOrWhiteSpace(search))
        {
            var literal = new BsonRegularExpression(Regex.Escape(search.Trim()), "i");
            filter &= builder.Or(builder.Regex(user => user.Id, literal), builder.Regex(user => user.FullName, literal));
        }
        return filter;
    }

    public async Task<(List<Prosumer> Items, long Total)> ListAsync(int page, int pageSize, AccountStatus? status,
        RequestStatus? requestStatus, string? search, CancellationToken ct)
    {
        var filter = CreateFilter(status, requestStatus, search);
        var total = await users.CountDocumentsAsync(filter, cancellationToken: ct);
        var items = await users.Find(filter).SortByDescending(user => user.CreatedAt).ThenBy(user => user.Id)
            .Skip((page - 1) * pageSize).Limit(pageSize).ToListAsync(ct);
        return (items, total);
    }

    public Task<long> CountAsync(AccountStatus? status, RequestStatus? requestStatus, CancellationToken ct) =>
        users.CountDocumentsAsync(CreateFilter(status, requestStatus), cancellationToken: ct);

    public async Task InsertAsync(Prosumer prosumer, CancellationToken ct)
    {
        try
        {
            await users.InsertOneAsync(prosumer, cancellationToken: ct);
        }
        catch (MongoWriteException e) when (e.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            throw new DuplicateAccountException();
        }
    }

    public async Task<bool> ReplaceAsync(Prosumer prosumer, long expectedVersion, CancellationToken ct)
    {
        var result = await users.ReplaceOneAsync(user => user.Id == prosumer.Id && user.Version == expectedVersion,
            prosumer, cancellationToken: ct);
        return result.ModifiedCount == 1;
    }

    public Task RevokeSessionsAsync(string nic, CancellationToken ct) => users.UpdateOneAsync(user => user.Id == nic,
        Builders<Prosumer>.Update.Inc(user => user.SecurityVersion, 1).Inc(user => user.Version, 1)
            .Set(user => user.UpdatedAt, DateTime.UtcNow), cancellationToken: ct);
}
