using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Api.Configuration;

public sealed class MongoOptions
{
    [Required] public string ConnectionString { get; set; } = "mongodb://127.0.0.1:27017";
    [Required, RegularExpression("^[a-zA-Z0-9_-]+$")] public string DatabaseName { get; set; } = "solar_microgrid";
}

public sealed class JwtOptions
{
    [Required] public string Issuer { get; set; } = "solar-microgrid-api";
    [Required] public string Audience { get; set; } = "solar-microgrid-clients";
    [Required, MinLength(32)] public string SigningKey { get; set; } = "";
    [Range(5, 60)] public int LifetimeMinutes { get; set; } = 30;
}
