using Rento.Application.DTOs.Auth;

namespace Rento.Application.Services;

/// <summary>
/// Authentication service: OTP send/verify, token refresh, logout.
/// </summary>
public interface IAuthService
{
    /// <summary>Sends OTP to the given phone. Throws <see cref="InvalidOperationException"/> with code AUTH_OTP_LIMIT when rate limit exceeded.</summary>
    Task<SendOtpResult> SendOtpAsync(SendOtpRequest request, CancellationToken ct = default);

    /// <summary>Verifies OTP and returns tokens. Throws on expired/invalid OTP or blocked user.</summary>
    Task<VerifyOtpResult> VerifyOtpAsync(VerifyOtpRequest request, CancellationToken ct = default);

    /// <summary>Refreshes access token using a valid refresh token. Throws on invalid token or blocked user.</summary>
    Task<TokenResult> RefreshTokenAsync(RefreshTokenRequest request, CancellationToken ct = default);

    /// <summary>Invalidates the refresh session for the user.</summary>
    Task LogoutAsync(Guid userId, CancellationToken ct = default);
}
