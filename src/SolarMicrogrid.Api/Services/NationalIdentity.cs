/**
 * @author Aenuka Buddhakorala
 * IT number: IT23214934
 * File functionality:
 * - Validates and normalizes Sri Lankan NIC values used to identify prosumer accounts during registration and login.
 */

using System.Text.RegularExpressions;

namespace SolarMicrogrid.Api.Services;

public static partial class NationalIdentity
{
    // Canonicalize legacy YYDDDSSSS[VX] into YYYYDDD0SSSS so both formats share one MongoDB _id.
    public static string Normalize(string value)
    {
        var nic = value.Trim().ToUpperInvariant();
        if (Legacy().IsMatch(nic))
        {
            nic = "19" + nic[..5] + "0" + nic.Substring(5, 4);
        }
        if (!Modern().IsMatch(nic))
        {
            throw ApiException.Invalid("Enter a 12-digit NIC or a 9-digit NIC ending in V or X.");
        }

        var year = int.Parse(nic[..4]);
        var day = int.Parse(nic.Substring(4, 3));
        if (day > 500)
        {
            day -= 500;
        }
        if (year < 1900 || year > DateTime.UtcNow.Year || day < 1 || day > 366)
        {
            throw ApiException.Invalid("The NIC contains an invalid year or day code.");
        }

        return nic;
    }

    [GeneratedRegex(@"^[0-9]{9}[VX]$", RegexOptions.CultureInvariant)]
    private static partial Regex Legacy();

    [GeneratedRegex(@"^[0-9]{12}$", RegexOptions.CultureInvariant)]
    private static partial Regex Modern();
}
