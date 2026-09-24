using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Api.Configuration;

public sealed class MongoOptions
{
    [Required]
    public string ConnectionString { get; set; } = "mongodb://127.0.0.1:27017";

    [Required, RegularExpression("^[a-zA-Z0-9_-]+$")]
    public string DatabaseName { get; set; } = "solar_microgrid";
}
