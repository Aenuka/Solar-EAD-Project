using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Contracts;

public sealed class ProsumerLoginRequest
{
    public ProsumerLoginRequest() { }
    public ProsumerLoginRequest(string nic, string password)
    {
        Nic = nic;
        Password = password;
    }

    [Required, StringLength(12)]
    public string Nic { get; set; } = "";

    [Required, StringLength(128)]
    public string Password { get; set; } = "";
}
