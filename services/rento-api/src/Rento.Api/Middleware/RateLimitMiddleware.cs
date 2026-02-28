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
            var count = await db.StringIncrementAsync(key);
            if (count == 1)
                await db.KeyExpireAsync(key, Window);

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
