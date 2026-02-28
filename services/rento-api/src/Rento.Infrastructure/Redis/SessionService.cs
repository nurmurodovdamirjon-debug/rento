using StackExchange.Redis;
using Rento.Application.Services;
using Rento.Core.Common;

namespace Rento.Infrastructure.Redis;

public class SessionService : ISessionService
{
    private const string KeyPrefix = "auth:session:";
    private const string RedisUnavailableMessage = "Redis unavailable. Auth temporarily disabled.";
    private readonly IConnectionMultiplexer _redis;

    public SessionService(IConnectionMultiplexer redis)
    {
        _redis = redis;
    }

    private IDatabase Db => _redis.GetDatabase();

    public async Task SetSessionAsync(Guid userId, string refreshToken, TimeSpan ttl, CancellationToken ct = default)
    {
        if (!_redis.IsConnected)
            throw new ServiceUnavailableException(RedisUnavailableMessage);
        var key = KeyPrefix + userId.ToString();
        await Db.StringSetAsync(key, refreshToken, ttl);
    }

    public async Task<string?> GetSessionAsync(Guid userId, CancellationToken ct = default)
    {
        if (!_redis.IsConnected)
            throw new ServiceUnavailableException(RedisUnavailableMessage);
        var key = KeyPrefix + userId.ToString();
        var val = await Db.StringGetAsync(key);
        return val.HasValue ? val.ToString() : null;
    }

    public async Task DeleteSessionAsync(Guid userId, CancellationToken ct = default)
    {
        if (!_redis.IsConnected)
            throw new ServiceUnavailableException(RedisUnavailableMessage);
        var key = KeyPrefix + userId.ToString();
        await Db.KeyDeleteAsync(key);
    }
}
