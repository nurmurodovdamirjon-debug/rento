using StackExchange.Redis;
using Rento.Application.Services;
using Rento.Core.Common;

namespace Rento.Infrastructure.Redis;

public class OtpService : IOtpService
{
    private const string KeyPrefix = "auth:otp:";
    private const string AttemptsPrefix = "auth:otp_attempts:";
    private const string RedisUnavailableMessage = "Redis unavailable. Auth temporarily disabled.";
    private readonly IConnectionMultiplexer _redis;

    public OtpService(IConnectionMultiplexer redis)
    {
        _redis = redis;
    }

    private IDatabase Db => _redis.GetDatabase();

    public async Task SetOtpAsync(string phone, string otp, TimeSpan ttl, CancellationToken ct = default)
    {
        if (!_redis.IsConnected)
            throw new ServiceUnavailableException(RedisUnavailableMessage);
        var key = KeyPrefix + phone;
        await Db.StringSetAsync(key, otp, ttl);
    }

    public async Task<string?> GetAndDeleteOtpAsync(string phone, CancellationToken ct = default)
    {
        if (!_redis.IsConnected)
            throw new ServiceUnavailableException(RedisUnavailableMessage);
        var key = KeyPrefix + phone;
        var value = await Db.StringGetAsync(key);
        if (value.HasValue)
            await Db.KeyDeleteAsync(key);
        return value.HasValue ? value.ToString() : null;
    }

    public async Task<int> GetAttemptsAsync(string phone, CancellationToken ct = default)
    {
        if (!_redis.IsConnected)
            throw new ServiceUnavailableException(RedisUnavailableMessage);
        var key = AttemptsPrefix + phone;
        var val = await Db.StringGetAsync(key);
        return val.HasValue && int.TryParse(val.ToString(), out var n) ? n : 0;
    }

    public async Task IncrementAttemptsAsync(string phone, TimeSpan window, int maxAttempts, CancellationToken ct = default)
    {
        if (!_redis.IsConnected)
            throw new ServiceUnavailableException(RedisUnavailableMessage);
        var key = AttemptsPrefix + phone;
        var count = await Db.StringIncrementAsync(key);
        if (count == 1)
            await Db.KeyExpireAsync(key, window);
    }

    public async Task<int> GetAttemptsRemainingAsync(string phone, int maxAttempts, CancellationToken ct = default)
    {
        if (!_redis.IsConnected)
            throw new ServiceUnavailableException(RedisUnavailableMessage);
        var current = await GetAttemptsAsync(phone, ct);
        return Math.Max(0, maxAttempts - current);
    }
}
