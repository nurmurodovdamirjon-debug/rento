using System.Net;
using System.Text.Json;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.Extensions.Logging;
using Rento.Core.Common;

namespace Rento.Api.Middleware;

/// <summary>
/// Global exception handler middleware. Maps exceptions to HTTP status and ApiResponse.
/// </summary>
public static class GlobalExceptionMiddleware
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        PropertyNameCaseInsensitive = true
    };

    /// <summary>
    /// Registers the global exception handler in the pipeline.
    /// </summary>
    public static IApplicationBuilder UseGlobalExceptionHandler(this IApplicationBuilder app)
    {
        var loggerFactory = app.ApplicationServices.GetRequiredService<ILoggerFactory>();
        var logger = loggerFactory.CreateLogger("GlobalExceptionHandler");

        app.UseExceptionHandler(errorApp =>
        {
            errorApp.Run(async context =>
            {
                context.Response.ContentType = "application/json";
                context.Response.StatusCode = (int)HttpStatusCode.InternalServerError;

                var feature = context.Features.Get<IExceptionHandlerFeature>();
                var ex = feature?.Error;
                if (ex != null)
                {
                    logger.LogError(ex, "Unhandled exception: {ExType} {ExMessage}", ex.GetType().Name, ex.Message);

                    if (ex is ServiceUnavailableException)
                    {
                        context.Response.StatusCode = (int)HttpStatusCode.ServiceUnavailable;
                        await WriteResponseAsync(context, ErrorCodes.ServiceUnavailable, ex.Message);
                        return;
                    }

                    if (ex is InvalidOperationException ioex)
                    {
                        context.Response.StatusCode = ioex.Message switch
                        {
                            "AUTH_OTP_LIMIT" => (int)HttpStatusCode.TooManyRequests,
                            "AUTH_TOKEN_INVALID" => (int)HttpStatusCode.Unauthorized,
                            "USER_BLOCKED" => (int)HttpStatusCode.Forbidden,
                            "AUTH_OTP_EXPIRED" or "AUTH_OTP_INVALID" => (int)HttpStatusCode.BadRequest,
                            _ => (int)HttpStatusCode.BadRequest
                        };
                        await WriteResponseAsync(context, ioex.Message, ioex.Message);
                        return;
                    }

                    if (ex is UnauthorizedAccessException)
                    {
                        context.Response.StatusCode = (int)HttpStatusCode.Unauthorized;
                        await WriteResponseAsync(context, ErrorCodes.Unauthorized, "Unauthorized");
                        return;
                    }

                    if (ex is ArgumentException argEx)
                    {
                        context.Response.StatusCode = (int)HttpStatusCode.BadRequest;
                        await WriteResponseAsync(context, ErrorCodes.ValidationError, argEx.Message);
                        return;
                    }
                }

                await WriteResponseAsync(context, ErrorCodes.InternalError, "Internal server error");
            });
        });

        return app;
    }

    private static async Task WriteResponseAsync(HttpContext context, string code, string message)
    {
        var response = new ApiResponse
        {
            Success = false,
            Error = new ApiError { Code = code, Message = message }
        };
        await context.Response.WriteAsync(JsonSerializer.Serialize(response, JsonOptions));
    }
}
