using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Web.ViewModels;

public sealed class LoginViewModel
{
    [Required, StringLength(50)]
    public string Username { get; set; } = "";

    [Required, DataType(DataType.Password), StringLength(128)]
    public string Password { get; set; } = "";
}
