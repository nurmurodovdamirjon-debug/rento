using System.Text.Json;
using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using Microsoft.Extensions.Diagnostics.HealthChecks;

namespace Rento.Api.Middleware;

public static class HealthCheckResponseWriter
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower
    };

    public static Task WriteResponse(HttpContext context, HealthReport report)
    {
        context.Response.ContentType = "application/json";

        var status = report.Status == HealthStatus.Healthy ? "ok" : "degraded";
        if (report.Status == HealthStatus.Unhealthy)
            context.Response.StatusCode = StatusCodes.Status503ServiceUnavailable;

        var checks = new Dictionary<string, string>();
        foreach (var entry in report.Entries)
            checks[entry.Key] = entry.Value.Status == HealthStatus.Healthy ? "ok" : "error";

        var response = new
        {
            status,
            service = "rento-api",
            version = context.RequestServices.GetService<IConfiguration>()?["App:Version"] ?? "1.0.0",
            checks
        };

        return context.Response.WriteAsync(JsonSerializer.Serialize(response, JsonOptions));
    }
}
