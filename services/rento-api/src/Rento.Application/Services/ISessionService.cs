namespace Rento.Application.Services;

public interface ISessionService
{
    Task SetSessionAsync(Guid userId, string refreshToken, TimeSpan ttl, CancellationToken ct = default);
    Task<string?> GetSessionAsync(Guid userId, CancellationToken ct = default);
    Task DeleteSessionAsync(Guid userId, CancellationToken ct = default);
}
