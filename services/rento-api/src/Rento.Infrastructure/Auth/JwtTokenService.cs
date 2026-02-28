using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.Tokens;
using Rento.Application.Services;

namespace Rento.Infrastructure.Auth;

public class JwtTokenService : IJwtTokenService
{
    private readonly string _accessSecret;
    private readonly string _refreshSecret;
    private const string Issuer = "rento.uz";
    private readonly int _accessExpiryMinutes;
    private readonly int _refreshExpiryDays;

    public JwtTokenService(IConfiguration configuration)
    {
        _accessSecret = configuration["Jwt:AccessSecret"] ?? throw new InvalidOperationException("Jwt:AccessSecret not set");
        _refreshSecret = configuration["Jwt:RefreshSecret"] ?? throw new InvalidOperationException("Jwt:RefreshSecret not set");
        _accessExpiryMinutes = int.Parse(configuration["Jwt:AccessExpiryMinutes"] ?? "15");
        _refreshExpiryDays = int.Parse(configuration["Jwt:RefreshExpiryDays"] ?? "7");
    }

    public int GetAccessExpirySeconds() => _accessExpiryMinutes * 60;

    public string GenerateAccessToken(Guid userId, string role, string phone)
    {
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_accessSecret));
        var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);
        var claims = new[]
        {
            new Claim(JwtRegisteredClaimNames.Sub, userId.ToString()),
            new Claim("role", role),
            new Claim("phone", phone),
            new Claim(JwtRegisteredClaimNames.Iss, Issuer)
        };
        var token = new JwtSecurityToken(
            issuer: Issuer,
            claims: claims,
            expires: DateTime.UtcNow.AddMinutes(_accessExpiryMinutes),
            signingCredentials: creds
        );
        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public string GenerateRefreshToken(Guid userId, string role, string phone)
    {
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_refreshSecret));
        var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);
        var claims = new[]
        {
            new Claim(JwtRegisteredClaimNames.Sub, userId.ToString()),
            new Claim("role", role),
            new Claim("phone", phone),
            new Claim(JwtRegisteredClaimNames.Iss, Issuer),
            new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),
            new Claim("type", "refresh")
        };
        var token = new JwtSecurityToken(
            issuer: Issuer,
            claims: claims,
            expires: DateTime.UtcNow.AddDays(_refreshExpiryDays),
            signingCredentials: creds
        );
        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public (Guid UserId, string Role, string Phone)? ValidateAccessToken(string token)
    {
        return ValidateToken(token, _accessSecret);
    }

    public (Guid UserId, string Role, string Phone)? ValidateRefreshToken(string token)
    {
        return ValidateToken(token, _refreshSecret);
    }

    private static (Guid UserId, string Role, string Phone)? ValidateToken(string token, string secret)
    {
        try
        {
            var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secret));
            var handler = new JwtSecurityTokenHandler();
            var validation = new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,
                IssuerSigningKey = key,
                ValidIssuer = Issuer,
                ValidateIssuer = true,
                ValidateAudience = false,
                ValidateLifetime = true,
                ClockSkew = TimeSpan.Zero
            };
            var principal = handler.ValidateToken(token, validation, out _);
            var sub = principal.FindFirst(ClaimTypes.NameIdentifier)?.Value ?? principal.FindFirst("sub")?.Value;
            var role = principal.FindFirst("role")?.Value ?? "";
            var phone = principal.FindFirst("phone")?.Value ?? "";
            if (string.IsNullOrEmpty(sub) || !Guid.TryParse(sub, out var userId))
                return null;
            return (userId, role, phone);
        }
        catch
        {
            return null;
        }
    }
}
