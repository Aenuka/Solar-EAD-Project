using System.Globalization;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using SolarMicrogrid.Api.Configuration;
using SolarMicrogrid.Api.Models;
using SolarMicrogrid.Contracts;

namespace SolarMicrogrid.Api.Security;

public sealed class TokenService(IOptions<JwtOptions> options)
{
    public AuthResponse Create(AccountDocument user)
    {
        var config = options.Value;
        var now = DateTimeOffset.UtcNow;
        var expires = now.AddMinutes(config.LifetimeMinutes);
        var claims = new[]
        {
            new Claim("sub", user.Id),
            new Claim("name", user.FullName),
            new Claim("role", user.Role),
            new Claim("sv", user.SecurityVersion.ToString(CultureInfo.InvariantCulture)),
            new Claim("jti", Guid.NewGuid().ToString("N")),
            new Claim("iat", now.ToUnixTimeSeconds().ToString(CultureInfo.InvariantCulture), ClaimValueTypes.Integer64)
        };
        var signingKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(config.SigningKey));
        var credentials = new SigningCredentials(signingKey, SecurityAlgorithms.HmacSha256);
        var token = new JwtSecurityToken(
            issuer: config.Issuer,
            audience: config.Audience,
            claims: claims,
            notBefore: now.UtcDateTime,
            expires: expires.UtcDateTime,
            signingCredentials: credentials);

        var accessToken = new JwtSecurityTokenHandler().WriteToken(token);
        return new AuthResponse(accessToken, expires, "Bearer", user.Id, user.FullName, user.Role);
    }
}
