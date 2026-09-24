using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Api.Data;
using SolarMicrogrid.Api.Security;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Services;

public sealed class AuthService(StaffRepository staff, ProsumerRepository prosumers, PasswordService passwords, TokenService tokens)
{
    public async Task<AuthResponse> LoginStaffAsync(StaffLoginRequest request, CancellationToken ct)
    {
        var user = await staff.FindByUsernameAsync(request.Username.Trim().ToLowerInvariant(), ct);
        return Authenticate(user, request.Password);
    }

    public async Task<AuthResponse> LoginProsumerAsync(ProsumerLoginRequest request, CancellationToken ct)
    {
        string nic;
        try
        {
            nic = NationalIdentity.Normalize(request.Nic);
        }
        catch (ApiException)
        {
            passwords.Verify(null, request.Password);
            throw ApiException.InvalidCredentials();
        }
        return Authenticate(await prosumers.FindAsync(nic, ct), request.Password);
    }

    private AuthResponse Authenticate(AccountDocument? user, string password)
    {
        if (!passwords.Verify(user, password) || user is null || user.Status != AccountStatus.Active)
        {
            throw ApiException.InvalidCredentials();
        }
        return tokens.Create(user);
    }

    public Task LogoutAsync(string id, string role, CancellationToken ct)
    {
        if (role == Roles.Prosumer)
        {
            return prosumers.RevokeSessionsAsync(id, ct);
        }

        return staff.RevokeSessionsAsync(id, ct);
    }

    public async Task<AccountDocument?> FindAccountAsync(string id, string role, CancellationToken ct)
    {
        if (role == Roles.Prosumer)
        {
            return await prosumers.FindAsync(id, ct);
        }

        return await staff.FindByIdAsync(id, ct);
    }
}
