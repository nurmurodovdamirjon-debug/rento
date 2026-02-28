namespace Rento.Application.Services;

public interface IJwtTokenService
{
    string GenerateAccessToken(Guid userId, string role, string phone);
    string GenerateRefreshToken(Guid userId, string role, string phone);
    (Guid UserId, string Role, string Phone)? ValidateAccessToken(string token);
    (Guid UserId, string Role, string Phone)? ValidateRefreshToken(string token);
    int GetAccessExpirySeconds();
}
