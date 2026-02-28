using Microsoft.AspNetCore.Mvc;
using Rento.Api.Extensions;
using Rento.Application.DTOs.Auth;
using Rento.Application.Services;
using Rento.Core.Common;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/auth")]
public class AuthController : ControllerBase
{
    private readonly IAuthService _authService;

    public AuthController(IAuthService authService)
    {
        _authService = authService;
    }

    [HttpPost("send-otp")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status429TooManyRequests)]
    public async Task<IActionResult> SendOtp([FromBody] SendOtpRequest request, CancellationToken ct)
    {
        try
        {
            var result = await _authService.SendOtpAsync(request, ct);
            return Ok(new ApiResponse { Success = true, Data = result });
        }
        catch (InvalidOperationException ex) when (ex.Message == "AUTH_OTP_LIMIT")
        {
            return StatusCode(429, new ApiResponse
            {
                Success = false,
                Error = new ApiError { Code = ErrorCodes.RateLimitExceeded, Message = "SMS limit exceeded" }
            });
        }
    }

    [HttpPost("verify-otp")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status403Forbidden)]
    public async Task<IActionResult> VerifyOtp([FromBody] VerifyOtpRequest request, CancellationToken ct)
    {
        try
        {
            var result = await _authService.VerifyOtpAsync(request, ct);
            return Ok(new ApiResponse { Success = true, Data = result });
        }
        catch (InvalidOperationException ex)
        {
            return ex.Message switch
            {
                "AUTH_OTP_EXPIRED" => BadRequest(new ApiResponse
                {
                    Success = false,
                    Error = new ApiError { Code = "AUTH_OTP_EXPIRED", Message = "OTP expired" }
                }),
                "AUTH_OTP_INVALID" => BadRequest(new ApiResponse
                {
                    Success = false,
                    Error = new ApiError { Code = "AUTH_OTP_INVALID", Message = "Invalid OTP" }
                }),
                "USER_BLOCKED" => StatusCode(403, new ApiResponse
                {
                    Success = false,
                    Error = new ApiError { Code = ErrorCodes.UserBlocked, Message = "User is blocked" }
                }),
                _ => BadRequest(new ApiResponse
                {
                    Success = false,
                    Error = new ApiError { Code = ex.Message, Message = ex.Message }
                })
            };
        }
    }

    [HttpPost("refresh-token")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status403Forbidden)]
    public async Task<IActionResult> RefreshToken([FromBody] RefreshTokenRequest request, CancellationToken ct)
    {
        try
        {
            var result = await _authService.RefreshTokenAsync(request, ct);
            return Ok(new ApiResponse { Success = true, Data = result });
        }
        catch (InvalidOperationException ex) when (ex.Message == "AUTH_TOKEN_INVALID")
        {
            return Unauthorized(new ApiResponse
            {
                Success = false,
                Error = new ApiError { Code = ErrorCodes.AuthTokenInvalid, Message = "Invalid or expired refresh token" }
            });
        }
        catch (InvalidOperationException ex) when (ex.Message == "USER_BLOCKED")
        {
            return StatusCode(403, new ApiResponse
            {
                Success = false,
                Error = new ApiError { Code = ErrorCodes.UserBlocked, Message = "User is blocked" }
            });
        }
    }

    [HttpPost("logout")]
    [Microsoft.AspNetCore.Authorization.Authorize]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> Logout(CancellationToken ct)
    {
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });

        await _authService.LogoutAsync(userId.Value, ct);
        return NoContent();
    }
}
