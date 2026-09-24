using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public sealed class ReactivateRequest
{
    public ReactivateRequest() { }
    public ReactivateRequest(string note, long version)
    {
        Note = note;
        Version = version;
    }

    [Required, StringLength(500, MinimumLength = 5)]
    public string Note { get; set; } = "";

    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}
