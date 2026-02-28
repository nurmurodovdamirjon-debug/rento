using System.Net;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using Rento.Api.Middleware;
using Rento.Core.Common;
using Xunit;

namespace Rento.Tests.Middleware;

public sealed class GlobalExceptionMiddlewareTests
{
    [Fact]
    public async Task UseGlobalExceptionHandler_WhenAuthTokenInvalid_Returns401()
    {
        using var server = CreateServer(() => throw new InvalidOperationException("AUTH_TOKEN_INVALID"));
        using var client = server.CreateClient();

        var response = await client.GetAsync("/");
        var body = await response.Content.ReadAsStringAsync();

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
        Assert.Contains("AUTH_TOKEN_INVALID", body);
    }

    [Fact]
    public async Task UseGlobalExceptionHandler_WhenServiceUnavailable_Returns503()
    {
        using var server = CreateServer(() => throw new ServiceUnavailableException("redis down"));
        using var client = server.CreateClient();

        var response = await client.GetAsync("/");
        var body = await response.Content.ReadAsStringAsync();

        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        Assert.Contains(ErrorCodes.ServiceUnavailable, body);
    }

    private static TestServer CreateServer(Action action)
    {
        var builder = new WebHostBuilder()
            .ConfigureServices(services =>
            {
                services.AddLogging();
            })
            .Configure(app =>
            {
                app.UseGlobalExceptionHandler();
                app.Run(_ =>
                {
                    action();
                    return Task.CompletedTask;
                });
            });

        return new TestServer(builder);
    }
}
