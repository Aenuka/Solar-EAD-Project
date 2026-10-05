/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Defines and validates staff profile, role, status, and version updates.
 */

using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public sealed class UpdateStaffRequest
{
    public UpdateStaffRequest() { }
    public UpdateStaffRequest(string fullName, string email, StaffRole? role, AccountStatus? status, long version)
    {
        FullName = fullName;
        Email = email;
        Role = role;
        Status = status;
        Version = version;
    }

    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";

    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";

    [Required]
    public StaffRole? Role { get; set; }

    [Required]
    public AccountStatus? Status { get; set; }

    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}
