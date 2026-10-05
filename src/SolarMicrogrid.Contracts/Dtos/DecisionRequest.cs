/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Defines and validates a Backoffice deactivation-review decision, note, and account version.
 */

using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public sealed class DecisionRequest
{
    public DecisionRequest() { }
    public DecisionRequest(Decision? decision, string note, long version)
    {
        Decision = decision;
        Note = note;
        Version = version;
    }

    [Required]
    public Decision? Decision { get; set; }

    [Required, StringLength(500, MinimumLength = 5)]
    public string Note { get; set; } = "";

    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}
