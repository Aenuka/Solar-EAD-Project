/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Defines and validates the identity, password, and role required to create a staff account.
 */

using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Contracts;

public sealed class CreateStaffRequest
{
    public CreateStaffRequest() { }
    public CreateStaffRequest(string username, string fullName, string email, string password, StaffRole? role)
    {
        Username = username;
        FullName = fullName;
        Email = email;
        Password = password;
        Role = role;
    }

    [Required, RegularExpression(@"^[a-zA-Z0-9._-]{3,50}$")]
    public string Username { get; set; } = "";

    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";

    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";

    [Required, StringLength(128, MinimumLength = 12)]
    public string Password { get; set; } = "";

    [Required]
    public StaffRole? Role { get; set; }
}
