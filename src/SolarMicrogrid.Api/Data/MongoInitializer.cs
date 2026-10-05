/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Initializes account collections, role validation, account indexes, and the protected bootstrap Backoffice user.
 */

using System.ComponentModel.DataAnnotations;
using MongoDB.Bson;
using MongoDB.Driver;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Api.Security;
using SolarMicrogrid.Api.Services;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Data;

public sealed class MongoInitializer(IMongoDatabase database, StaffRepository staff,
    PasswordService passwords, IConfiguration configuration, ILogger<MongoInitializer> logger)
{
    public async Task InitializeAsync(CancellationToken ct)
    {
        await EnsureCollectionAsync("staffUsers", [Roles.Backoffice, Roles.GridOperator], ct);
        await EnsureCollectionAsync("prosumers", [Roles.Prosumer], ct);
        await database.GetCollection<StaffUser>("staffUsers").Indexes.CreateOneAsync(
            new CreateIndexModel<StaffUser>(Builders<StaffUser>.IndexKeys.Ascending(x => x.Username),
                new CreateIndexOptions { Unique = true, Name = "unique_username" }), cancellationToken: ct);
        await database.GetCollection<Prosumer>("prosumers").Indexes.CreateManyAsync(
        [
            new(Builders<Prosumer>.IndexKeys.Ascending(x => x.Status)),
            new(Builders<Prosumer>.IndexKeys.Ascending("deactivationRequest.status"))
        ], ct);

        var username = configuration["Bootstrap:Username"];
        var password = configuration["Bootstrap:Password"];
        if (string.IsNullOrWhiteSpace(username) && string.IsNullOrEmpty(password))
        {
            if ((await staff.ListAsync(1, 1, ct)).Total == 0)
            {
                logger.LogWarning("No staff accounts exist. Configure Bootstrap settings once to create the initial Backoffice account.");
            }
            return;
        }
        // Never reset an existing account's password when the API restarts.
        if (await staff.FindByIdAsync("bootstrap", ct) is not null)
        {
            return;
        }
        var request = new CreateStaffRequest(username ?? "", configuration["Bootstrap:FullName"] ?? "Grid Administrator",
            configuration["Bootstrap:Email"] ?? "", password ?? "", StaffRole.Backoffice);
        Validator.ValidateObject(request, new ValidationContext(request), validateAllProperties: true);
        var user = new StaffUser
        {
            Id = "bootstrap",
            Username = request.Username.Trim().ToLowerInvariant(),
            FullName = request.FullName.Trim(),
            Email = request.Email.Trim(),
            Role = Roles.Backoffice,
            IsProtected = true
        };
        user.PasswordHash = passwords.Hash(user, request.Password);
        user.RecentEvents = user.WithEvent("Bootstrapped", "system");
        try
        {
            await staff.InsertAsync(user, ct);
        }
        catch (DuplicateAccountException)
        {
            // Another API instance may have initialized the same account concurrently.
            if (await staff.FindByIdAsync("bootstrap", ct) is null)
            {
                throw new InvalidOperationException("Bootstrap username is already assigned to another account.");
            }
        }
    }

    private async Task EnsureCollectionAsync(string name, string[] roles, CancellationToken ct)
    {
        var schema = new BsonDocument
        {
            ["bsonType"] = "object",
            ["required"] = new BsonArray { "_id", "fullName", "email", "passwordHash", "role", "status", "version", "securityVersion", "createdAt" },
            ["properties"] = new BsonDocument
            {
                ["_id"] = new BsonDocument("bsonType", "string"),
                ["passwordHash"] = new BsonDocument { ["bsonType"] = "string", ["minLength"] = 20 },
                ["role"] = new BsonDocument("enum", new BsonArray(roles)),
                ["status"] = new BsonDocument("enum", new BsonArray { "Active", "Inactive" }),
                ["version"] = new BsonDocument { ["bsonType"] = "long", ["minimum"] = 1 },
                ["securityVersion"] = new BsonDocument { ["bsonType"] = "long", ["minimum"] = 1 },
                ["createdAt"] = new BsonDocument("bsonType", "date")
            }
        };
        var validator = new BsonDocument("$jsonSchema", schema);
        try
        {
            await database.CreateCollectionAsync(name,
                new CreateCollectionOptions<BsonDocument> { Validator = validator }, ct);
        }
        catch (MongoCommandException e) when (e.Code == 48)
        {
            // The collection already exists.
        }
    }
}
