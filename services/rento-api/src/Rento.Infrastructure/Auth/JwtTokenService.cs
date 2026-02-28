using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Microsoft.IdentityModel.Tokens;
using Rento.Application.Services;

namespace Rento.Infrastructure.Auth;

public class JwtTokenService : IJwtTokenService
{
    private const string Issuer = "rento.uz";
    private const string Audience = "rento-api";

    private readonly SymmetricSecurityKey _accessKey;
    private readonly SymmetricSecurityKey _refreshKey;
    private readonly int _accessExpiryMinutes;
    private readonly int _refreshExpiryDays;
    private readonly ILogger<JwtTokenService> _logger;

    public JwtTokenService(IConfiguration configuration, ILogger<JwtTokenService> logger)
    {
        _logger = logger;
        var accessSecret = configuration["Jwt:AccessSecret"]
            ?? throw new InvalidOperationException("Jwt:AccessSecret not set");
        var refreshSecret = configuration["Jwt:RefreshSecret"]
            ?? throw new InvalidOperationException("Jwt:RefreshSecret not set");

        _accessKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(accessSecret));
        _refreshKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(refreshSecret));
        _accessExpiryMinutes = int.Parse(configuration["Jwt:AccessExpiryMinutes"] ?? "15");
        _refreshExpiryDays = int.Parse(configuration["Jwt:RefreshExpiryDays"] ?? "7");
    }

    public int GetAccessExpirySeconds() => _accessExpiryMinutes * 60;

    public string GenerateAccessToken(Guid userId, string role)
    {
        var creds = new SigningCredentials(_accessKey, SecurityAlgorithms.HmacSha256);
        var claims = new[]
        {
            new Claim(JwtRegisteredClaimNames.Sub, userId.ToString()),
            new Claim("role", role),
            new Claim(JwtRegisteredClaimNames.Iss, Issuer),
            new Claim(JwtRegisteredClaimNames.Aud, Audience)
        };
        var token = new JwtSecurityToken(
            issuer: Issuer,
            audience: Audience,
            claims: claims,
            expires: DateTime.UtcNow.AddMinutes(_accessExpiryMinutes),
            signingCredentials: creds
        );
        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public string GenerateRefreshToken(Guid userId, string role)
    {
        var creds = new SigningCredentials(_refreshKey, SecurityAlgorithms.HmacSha256);
        var claims = new[]
        {
            new Claim(JwtRegisteredClaimNames.Sub, userId.ToString()),
            new Claim("role", role),
            new Claim(JwtRegisteredClaimNames.Iss, Issuer),
            new Claim(JwtRegisteredClaimNames.Aud, Audience),
            new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),
            new Claim("type", "refresh")
        };
        var token = new JwtSecurityToken(
            issuer: Issuer,
            audience: Audience,
            claims: claims,
            expires: DateTime.UtcNow.AddDays(_refreshExpiryDays),
            signingCredentials: creds
        );
        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    public (Guid UserId, string Role)? ValidateAccessToken(string token)
        => ValidateToken(token, _accessKey);

    public (Guid UserId, string Role)? ValidateRefreshToken(string token)
        => ValidateToken(token, _refreshKey);

    private (Guid UserId, string Role)? ValidateToken(string token, SymmetricSecurityKey key)
    {
        try
        {
            var handler = new JwtSecurityTokenHandler();
            var validation = new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,
                IssuerSigningKey = key,
                ValidIssuer = Issuer,
                ValidAudience = Audience,
                ValidateIssuer = true,
                ValidateAudience = true,
                ValidateLifetime = true,
                ClockSkew = TimeSpan.Zero
            };
            var principal = handler.ValidateToken(token, validation, out _);
            var sub = principal.FindFirst(ClaimTypes.NameIdentifier)?.Value
                      ?? principal.FindFirst("sub")?.Value;
            var role = principal.FindFirst("role")?.Value ?? "";
            if (string.IsNullOrEmpty(sub) || !Guid.TryParse(sub, out var userId))
                return null;
            return (userId, role);
        }
        catch (SecurityTokenException ex)
        {
            _logger.LogWarning(ex, "JWT token validation failed");
            return null;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Unexpected error during JWT token validation");
            return null;
        }
    }
}
