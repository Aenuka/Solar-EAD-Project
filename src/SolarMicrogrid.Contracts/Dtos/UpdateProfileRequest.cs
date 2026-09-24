using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace SolarMicrogrid.Contracts;

public sealed class UpdateProfileRequest
{
    public UpdateProfileRequest() { }
    public UpdateProfileRequest(string fullName, string email, string phone, string address, long version)
    {
        FullName = fullName;
        Email = email;
        Phone = phone;
        Address = address;
        Version = version;
    }

    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";

    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";

    [Required, RegularExpression(@"^\+?[0-9][0-9 -]{6,19}$")]
    public string Phone { get; set; } = "";

    [Required, StringLength(300, MinimumLength = 5)]
    public string Address { get; set; } = "";

    [JsonRequired, Range(1, long.MaxValue)]
    public long Version { get; set; }
}
