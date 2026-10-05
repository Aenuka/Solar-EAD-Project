/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Persists and queries staff accounts in MongoDB, handles duplicate usernames, and revokes sessions.
 */

using MongoDB.Driver;
using SolarMicrogrid.Api.Models;

namespace SolarMicrogrid.Api.Data;

public sealed class StaffRepository(IMongoDatabase database)
{
    private readonly IMongoCollection<StaffUser> users = database.GetCollection<StaffUser>("staffUsers");

    public async Task<StaffUser?> FindByIdAsync(string id, CancellationToken ct) =>
        await users.Find(user => user.Id == id).FirstOrDefaultAsync(ct);

    public async Task<StaffUser?> FindByUsernameAsync(string username, CancellationToken ct) =>
        await users.Find(user => user.Username == username).FirstOrDefaultAsync(ct);

    public async Task<(List<StaffUser> Items, long Total)> ListAsync(int page, int pageSize, CancellationToken ct)
    {
        var total = await users.CountDocumentsAsync(FilterDefinition<StaffUser>.Empty, cancellationToken: ct);
        var items = await users.Find(FilterDefinition<StaffUser>.Empty).SortBy(user => user.Username)
            .Skip((page - 1) * pageSize).Limit(pageSize).ToListAsync(ct);
        return (items, total);
    }

    public async Task InsertAsync(StaffUser user, CancellationToken ct)
    {
        try
        {
            await users.InsertOneAsync(user, cancellationToken: ct);
        }
        catch (MongoWriteException e) when (e.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            throw new DuplicateAccountException();
        }
    }

    public async Task<bool> ReplaceAsync(StaffUser user, long expectedVersion, CancellationToken ct)
    {
        var result = await users.ReplaceOneAsync(existing => existing.Id == user.Id && existing.Version == expectedVersion,
            user, cancellationToken: ct);
        return result.ModifiedCount == 1;
    }

    public Task RevokeSessionsAsync(string id, CancellationToken ct) => users.UpdateOneAsync(user => user.Id == id,
        Builders<StaffUser>.Update.Inc(user => user.SecurityVersion, 1).Inc(user => user.Version, 1)
            .Set(user => user.UpdatedAt, DateTime.UtcNow), cancellationToken: ct);
}
