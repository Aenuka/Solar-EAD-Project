using System.Security.Cryptography;
using Microsoft.AspNetCore.Identity;
using SolarMicrogrid.Api.Models;

namespace SolarMicrogrid.Api.Security;

public sealed class PasswordService
{
    private readonly PasswordHasher<AccountDocument> hasher = new();
    private readonly StaffUser dummy = new();
    private readonly string dummyHash;

    public PasswordService() => dummyHash = hasher.HashPassword(dummy, Convert.ToBase64String(RandomNumberGenerator.GetBytes(32)));

    public string Hash(AccountDocument user, string password) => hasher.HashPassword(user, password);

    public bool Verify(AccountDocument? user, string password)
    {
        // Unknown accounts still perform the same expensive password verification.
        var result = hasher.VerifyHashedPassword(user ?? dummy, user?.PasswordHash ?? dummyHash, password);
        return user is not null && result != PasswordVerificationResult.Failed;
    }
}
