using System.Net;
using System.Text.Json;
using Microsoft.AspNetCore.Diagnostics;
using Rento.Core.Common;

namespace Rento.Api.Middleware;

public static class GlobalExceptionMiddleware
{
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
        PropertyNameCaseInsensitive = true
    };

    public static IApplicationBuilder UseGlobalExceptionHandler(this IApplicationBuilder app)
    {
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
                    if (ex is Rento.Core.Common.ServiceUnavailableException)
                    {
                        context.Response.StatusCode = (int)HttpStatusCode.ServiceUnavailable;
                        var unavailableResponse = new ApiResponse
                        {
                            Success = false,
                            Error = new ApiError
                            {
                                Code = ErrorCodes.ServiceUnavailable,
                                Message = ex.Message
                            }
                        };
                        await context.Response.WriteAsync(JsonSerializer.Serialize(unavailableResponse, JsonOptions));
                        return;
                    }
                    // Log here if needed; do not expose internal details to client
                }

                var response = new ApiResponse
                {
                    Success = false,
                    Error = new ApiError
                    {
                        Code = ErrorCodes.InternalError,
                        Message = "Ichki server xatosi"
                    }
                };

                await context.Response.WriteAsync(JsonSerializer.Serialize(response, JsonOptions));
            });
        });

        return app;
    }
}
