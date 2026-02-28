namespace Rento.Application.Services;

public interface IJwtTokenService
{
    string GenerateAccessToken(Guid userId, string role);
    string GenerateRefreshToken(Guid userId, string role);
    (Guid UserId, string Role)? ValidateAccessToken(string token);
    (Guid UserId, string Role)? ValidateRefreshToken(string token);
    int GetAccessExpirySeconds();
}
