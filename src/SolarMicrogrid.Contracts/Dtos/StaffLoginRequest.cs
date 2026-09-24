using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Contracts;

public sealed class StaffLoginRequest
{
    public StaffLoginRequest() { }
    public StaffLoginRequest(string username, string password)
    {
        Username = username;
        Password = password;
    }

    [Required, StringLength(50)]
    public string Username { get; set; } = "";

    [Required, StringLength(128)]
    public string Password { get; set; } = "";
}
