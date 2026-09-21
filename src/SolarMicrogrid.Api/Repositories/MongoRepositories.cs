using System.Text.RegularExpressions;
using MongoDB.Bson;
using MongoDB.Driver;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Repositories;

public sealed class MongoStaffRepository(IMongoDatabase database) : IStaffRepository
{
    private readonly IMongoCollection<StaffUser> users = database.GetCollection<StaffUser>("staffUsers");

    public async Task<StaffUser?> FindByIdAsync(string id, CancellationToken ct) =>
        await users.Find(x => x.Id == id).FirstOrDefaultAsync(ct);

    public async Task<StaffUser?> FindByUsernameAsync(string username, CancellationToken ct) =>
        await users.Find(x => x.Username == username).FirstOrDefaultAsync(ct);

    public async Task<(List<StaffUser>, long)> ListAsync(int page, int size, CancellationToken ct)
    {
        var total = await users.CountDocumentsAsync(FilterDefinition<StaffUser>.Empty, cancellationToken: ct);
        var items = await users.Find(FilterDefinition<StaffUser>.Empty).SortBy(x => x.Username)
            .Skip((page - 1) * size).Limit(size).ToListAsync(ct);
        return (items, total);
    }

    public async Task InsertAsync(StaffUser user, CancellationToken ct)
    {
        try { await users.InsertOneAsync(user, cancellationToken: ct); }
        catch (MongoWriteException e) when (e.WriteError.Category == ServerErrorCategory.DuplicateKey)
        { throw new DuplicateAccountException(); }
    }

    public async Task<bool> ReplaceAsync(StaffUser user, long expectedVersion, CancellationToken ct)
    {
        var result = await users.ReplaceOneAsync(x => x.Id == user.Id && x.Version == expectedVersion,
            user, cancellationToken: ct);
        return result.ModifiedCount == 1;
    }

    public Task RevokeSessionsAsync(string id, CancellationToken ct) => users.UpdateOneAsync(x => x.Id == id,
        Builders<StaffUser>.Update.Inc(x => x.SecurityVersion, 1).Inc(x => x.Version, 1)
            .Set(x => x.UpdatedAt, DateTime.UtcNow), cancellationToken: ct);
}

public sealed class MongoProsumerRepository(IMongoDatabase database) : IProsumerRepository
{
    private readonly IMongoCollection<Prosumer> users = database.GetCollection<Prosumer>("prosumers");

    public async Task<Prosumer?> FindAsync(string nic, CancellationToken ct) =>
        await users.Find(x => x.Id == nic).FirstOrDefaultAsync(ct);

    private static FilterDefinition<Prosumer> Filter(AccountStatus? status, RequestStatus? requestStatus, string? search = null)
    {
        var f = Builders<Prosumer>.Filter;
        var filter = f.Empty;
        if (status.HasValue) filter &= f.Eq(x => x.Status, status.Value);
        if (requestStatus.HasValue) filter &= f.Eq("deactivationRequest.status", requestStatus.Value.ToString());
        if (!string.IsNullOrWhiteSpace(search))
        {
            var literal = new BsonRegularExpression(Regex.Escape(search.Trim()), "i");
            filter &= f.Or(f.Regex(x => x.Id, literal), f.Regex(x => x.FullName, literal));
        }
        return filter;
    }

    public async Task<(List<Prosumer>, long)> ListAsync(int page, int size, AccountStatus? status,
        RequestStatus? requestStatus, string? search, CancellationToken ct)
    {
        var filter = Filter(status, requestStatus, search);
        var total = await users.CountDocumentsAsync(filter, cancellationToken: ct);
        var items = await users.Find(filter).SortByDescending(x => x.CreatedAt).ThenBy(x => x.Id)
            .Skip((page - 1) * size).Limit(size).ToListAsync(ct);
        return (items, total);
    }

    public Task<long> CountAsync(AccountStatus? status, RequestStatus? requestStatus, CancellationToken ct) =>
        users.CountDocumentsAsync(Filter(status, requestStatus), cancellationToken: ct);

    public async Task InsertAsync(Prosumer prosumer, CancellationToken ct)
    {
        try { await users.InsertOneAsync(prosumer, cancellationToken: ct); }
        catch (MongoWriteException e) when (e.WriteError.Category == ServerErrorCategory.DuplicateKey)
        { throw new DuplicateAccountException(); }
    }

    public async Task<bool> ReplaceAsync(Prosumer prosumer, long expectedVersion, CancellationToken ct)
    {
        var result = await users.ReplaceOneAsync(x => x.Id == prosumer.Id && x.Version == expectedVersion,
            prosumer, cancellationToken: ct);
        return result.ModifiedCount == 1;
    }

    public Task RevokeSessionsAsync(string nic, CancellationToken ct) => users.UpdateOneAsync(x => x.Id == nic,
        Builders<Prosumer>.Update.Inc(x => x.SecurityVersion, 1).Inc(x => x.Version, 1)
            .Set(x => x.UpdatedAt, DateTime.UtcNow), cancellationToken: ct);
}
