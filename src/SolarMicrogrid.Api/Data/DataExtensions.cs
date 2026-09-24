using Microsoft.Extensions.Options;
using MongoDB.Bson.Serialization.Conventions;
using MongoDB.Driver;
using SolarMicrogrid.Api.Configuration;

namespace SolarMicrogrid.Api.Data;

// Reference: Julio Casal YouTube tutorials
// https://www.youtube.com/@juliocasal
public static class DataExtensions
{
    public static void AddMicrogridDatabase(this WebApplicationBuilder builder)
    {
        builder.Services.AddOptions<MongoOptions>()
            .BindConfiguration("Mongo")
            .ValidateDataAnnotations()
            .ValidateOnStart();

        ConventionRegistry.Register("solar-camel-case", new ConventionPack { new CamelCaseElementNameConvention() },
            type => type.Namespace == "SolarMicrogrid.Api.Models");
        builder.Services.AddSingleton<IMongoClient>(services =>
        {
            var options = services.GetRequiredService<IOptions<MongoOptions>>().Value;
            var settings = MongoClientSettings.FromConnectionString(options.ConnectionString);
            settings.ServerSelectionTimeout = TimeSpan.FromSeconds(5);
            return new MongoClient(settings);
        });
        builder.Services.AddSingleton(services =>
        {
            var client = services.GetRequiredService<IMongoClient>();
            var options = services.GetRequiredService<IOptions<MongoOptions>>().Value;
            return client.GetDatabase(options.DatabaseName);
        });
        builder.Services.AddScoped<StaffRepository>();
        builder.Services.AddScoped<ProsumerRepository>();
        builder.Services.AddScoped<StationRepository>();
        builder.Services.AddScoped<MongoInitializer>();
    }

    public static async Task InitializeDatabaseAsync(this WebApplication app)
    {
        await using var scope = app.Services.CreateAsyncScope();
        var initializer = scope.ServiceProvider.GetRequiredService<MongoInitializer>();
        await initializer.InitializeAsync(CancellationToken.None);
    }
}
