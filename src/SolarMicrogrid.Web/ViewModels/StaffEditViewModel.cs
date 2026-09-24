using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc.ModelBinding.Validation;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Web.ViewModels;

public sealed class StaffEditViewModel
{
    public string Id { get; set; } = "";

    [ValidateNever]
    public string Username { get; set; } = "";

    [ValidateNever]
    public bool IsProtected { get; set; }

    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";

    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";

    [Required]
    public StaffRole? Role { get; set; }

    [Required]
    public AccountStatus? Status { get; set; }

    [Range(1, long.MaxValue)]
    public long Version { get; set; }
}
