/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Defines and validates the NIC and password submitted for prosumer login.
 */

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
