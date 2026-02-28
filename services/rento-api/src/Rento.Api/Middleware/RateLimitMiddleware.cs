using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Configuration;
using StackExchange.Redis;

namespace Rento.Api.Middleware;

public static class RateLimitMiddleware
{
    private const string KeyPrefix = "ratelimit:global:";
    private const int DefaultMaxPerMinute = 60;
    private static readonly TimeSpan Window = TimeSpan.FromMinutes(1);

    // Atomically increment and set TTL on first call (no TOCTOU race condition)
    private const string IncrementScript =
        "local v = redis.call('INCR', KEYS[1]) " +
        "if v == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
        "return v";

    public static IApplicationBuilder UseRedisRateLimit(this IApplicationBuilder app)
    {
        app.Use(async (context, next) =>
        {
            var redis = context.RequestServices.GetService<IConnectionMultiplexer>();
            var config = context.RequestServices.GetService<IConfiguration>();
            if (redis == null || !redis.IsConnected)
            {
                await next();
                return;
            }

            var max = int.TryParse(config?["RateLimit:MaxRequestsPerMinute"], out var m) ? m : DefaultMaxPerMinute;
            var clientId = context.User?.FindFirst("sub")?.Value ?? context.Connection.RemoteIpAddress?.ToString() ?? "anon";
            var key = KeyPrefix + clientId;

            var db = redis.GetDatabase();
            var result = await db.ScriptEvaluateAsync(
                IncrementScript,
                keys: [(RedisKey)key],
                values: [(RedisValue)(long)Window.TotalSeconds]);
            var count = (long)result;

            if (count > max)
            {
                context.Response.StatusCode = 429;
                context.Response.ContentType = "application/json";
                await context.Response.WriteAsync("{\"success\":false,\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests\"}}");
                return;
            }

            await next();
        });
        return app;
    }
}
