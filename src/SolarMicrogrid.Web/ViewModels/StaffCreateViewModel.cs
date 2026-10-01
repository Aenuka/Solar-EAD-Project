using System.ComponentModel.DataAnnotations;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Web.ViewModels;

public sealed class StaffCreateViewModel
{
    [Required, RegularExpression(@"^[a-zA-Z0-9._-]{3,50}$")]
    public string Username { get; set; } = "";

    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";

    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";

    [System.Text.Json.Serialization.JsonIgnore]
    [Required, DataType(DataType.Password), StringLength(128, MinimumLength = 12)]
    public string Password { get; set; } = "";

    [Required]
    public StaffRole? Role { get; set; } = StaffRole.GridOperator;
}
