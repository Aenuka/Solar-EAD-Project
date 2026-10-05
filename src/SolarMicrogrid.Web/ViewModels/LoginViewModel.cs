/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Validates staff login form credentials and excludes the password from serialized page data.
 */

using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Web.ViewModels;

public sealed class LoginViewModel
{
    [Required, StringLength(50)]
    public string Username { get; set; } = "";

    [System.Text.Json.Serialization.JsonIgnore]
    [Required, DataType(DataType.Password), StringLength(128)]
    public string Password { get; set; } = "";
}
