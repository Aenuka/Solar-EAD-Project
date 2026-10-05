/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Validates prosumer profile edits and supplies account details to the management page.
 */

using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc.ModelBinding.Validation;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Web.ViewModels;

public sealed class ProsumerEditViewModel
{
    public string Nic { get; set; } = "";

    [Required, StringLength(100, MinimumLength = 2)]
    public string FullName { get; set; } = "";

    [Required, EmailAddress, StringLength(254)]
    public string Email { get; set; } = "";

    [Required, RegularExpression(@"^\+?[0-9][0-9 -]{6,19}$")]
    public string Phone { get; set; } = "";

    [Required, StringLength(300, MinimumLength = 5)]
    public string Address { get; set; } = "";

    [Range(1, long.MaxValue)]
    public long Version { get; set; }

    [ValidateNever]
    public ProsumerResponse Account { get; set; } = null!;
}
