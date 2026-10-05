/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Defines and validates the reason and account version submitted when requesting deactivation.
 */

using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public sealed class DeactivationRequestInput
{
    public DeactivationRequestInput() { }
    public DeactivationRequestInput(string reason, long version)
    {
        Reason = reason;
        Version = version;
    }

    [Required, StringLength(500, MinimumLength = 5)]
    public string Reason { get; set; } = "";

    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}
