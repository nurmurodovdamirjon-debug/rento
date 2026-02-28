using Rento.Application.DTOs.Auth;

namespace Rento.Application.Services;

public interface IAuthService
{
    Task<SendOtpResult> SendOtpAsync(SendOtpRequest request, CancellationToken ct = default);
    Task<VerifyOtpResult> VerifyOtpAsync(VerifyOtpRequest request, CancellationToken ct = default);
    Task<TokenResult> RefreshTokenAsync(RefreshTokenRequest request, CancellationToken ct = default);
    Task LogoutAsync(Guid userId, CancellationToken ct = default);
}
