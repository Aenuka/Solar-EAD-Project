using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc.ModelBinding.Validation;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Web.ViewModels;

public sealed class LoginViewModel
{
    [Required, StringLength(50)] public string Username { get; set; } = "";
    [Required, DataType(DataType.Password), StringLength(128)] public string Password { get; set; } = "";
}

public sealed class StaffCreateViewModel
{
    [Required, RegularExpression(@"^[a-zA-Z0-9._-]{3,50}$")] public string Username { get; set; } = "";
    [Required, StringLength(100, MinimumLength = 2)] public string FullName { get; set; } = "";
    [Required, EmailAddress, StringLength(254)] public string Email { get; set; } = "";
    [Required, DataType(DataType.Password), StringLength(128, MinimumLength = 12)] public string Password { get; set; } = "";
    [Required] public StaffRole? Role { get; set; } = StaffRole.GridOperator;
}

public sealed class StaffEditViewModel
{
    public string Id { get; set; } = "";
    [ValidateNever] public string Username { get; set; } = "";
    [ValidateNever] public bool IsProtected { get; set; }
    [Required, StringLength(100, MinimumLength = 2)] public string FullName { get; set; } = "";
    [Required, EmailAddress, StringLength(254)] public string Email { get; set; } = "";
    [Required] public StaffRole? Role { get; set; }
    [Required] public AccountStatus? Status { get; set; }
    [Range(1, long.MaxValue)] public long Version { get; set; }
}

public sealed class ProsumerEditViewModel
{
    public string Nic { get; set; } = "";
    [Required, StringLength(100, MinimumLength = 2)] public string FullName { get; set; } = "";
    [Required, EmailAddress, StringLength(254)] public string Email { get; set; } = "";
    [Required, RegularExpression(@"^\+?[0-9][0-9 -]{6,19}$")] public string Phone { get; set; } = "";
    [Required, StringLength(300, MinimumLength = 5)] public string Address { get; set; } = "";
    [Range(1, long.MaxValue)] public long Version { get; set; }
    [ValidateNever] public ProsumerResponse Account { get; set; } = null!;
}

public sealed record ErrorViewModel(int StatusCode, string Message);
