using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;
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
    public async Task<IActionResult> Logout(CancellationToken ct)
    {
        var userIdClaim = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub");
        if (string.IsNullOrEmpty(userIdClaim) || !Guid.TryParse(userIdClaim, out var userId))
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });

        await _authService.LogoutAsync(userId, ct);
        return NoContent();
    }
}
