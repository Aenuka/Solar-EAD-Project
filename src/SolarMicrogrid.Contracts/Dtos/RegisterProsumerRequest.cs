using System.ComponentModel.DataAnnotations;

namespace SolarMicrogrid.Contracts;

// Reference: Julio Casal YouTube tutorials
// https://www.youtube.com/@juliocasal
public sealed class RegisterProsumerRequest
{
    public RegisterProsumerRequest() { }
    public RegisterProsumerRequest(string nic, string fullName, string email, string phone, string address, string password)
    {
        Nic = nic;
        FullName = fullName;
        Email = email;
        Phone = phone;
        Address = address;
        Password = password;
    }

    [Required, StringLength(12)]
    public string Nic { get; set; } = "";

    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";

    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";

    [Required, RegularExpression(@"^\+?[0-9][0-9 -]{6,19}$")]
    public string Phone { get; set; } = "";

    [Required, StringLength(300, MinimumLength = 5)]
    public string Address { get; set; } = "";

    [Required, StringLength(128, MinimumLength = 12)]
    public string Password { get; set; } = "";
}
