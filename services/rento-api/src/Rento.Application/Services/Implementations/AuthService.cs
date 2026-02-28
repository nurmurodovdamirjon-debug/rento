using Rento.Application.DTOs.Auth;
using Rento.Application.Repositories;
using Rento.Application.Services;
using Rento.Application.Options;
using Rento.Core.Common;
using Rento.Core.Entities;

namespace Rento.Application.Services.Implementations;

/// <summary>
/// Handles OTP send/verify, token refresh and logout.
/// </summary>
public class AuthService : IAuthService
{
    private readonly IUserRepository _userRepo;
    private readonly IJwtTokenService _jwt;
    private readonly IOtpService _otp;
    private readonly ISessionService _session;
    private readonly ISmsService _sms;
    private readonly AuthOptions _options;

    public AuthService(
        IUserRepository userRepo,
        IJwtTokenService jwt,
        IOtpService otp,
        ISessionService session,
        ISmsService sms,
        Microsoft.Extensions.Options.IOptions<AuthOptions> options)
    {
        _userRepo = userRepo;
        _jwt = jwt;
        _otp = otp;
        _session = session;
        _sms = sms;
        _options = options?.Value ?? new AuthOptions();
    }

    /// <inheritdoc />
    public async Task<SendOtpResult> SendOtpAsync(SendOtpRequest request, CancellationToken ct = default)
    {
        var phone = request.Phone.Trim();
        var attempts = await _otp.GetAttemptsAsync(phone, ct);
        if (attempts >= _options.SmsMaxPerHour)
            throw new InvalidOperationException("AUTH_OTP_LIMIT");

        var otpCode = Random.Shared.Next(100000, 999999).ToString();
        await _otp.SetOtpAsync(phone, otpCode, TimeSpan.FromSeconds(_options.OtpExpirySeconds), ct);
        await _otp.IncrementAttemptsAsync(phone, TimeSpan.FromSeconds(_options.SmsWindowSeconds), _options.SmsMaxPerHour, ct);

        await _sms.SendOtpAsync(phone, otpCode, ct);

        var remaining = await _otp.GetAttemptsRemainingAsync(phone, _options.SmsMaxPerHour, ct);
        return new SendOtpResult
        {
            Phone = phone,
            ExpiresIn = _options.OtpExpirySeconds,
            RetryAfter = 60,
            AttemptsRemaining = remaining
        };
    }

    /// <inheritdoc />
    public async Task<VerifyOtpResult> VerifyOtpAsync(VerifyOtpRequest request, CancellationToken ct = default)
    {
        var phone = request.Phone.Trim();
        var storedOtp = await _otp.GetAndDeleteOtpAsync(phone, ct);
        if (storedOtp == null)
            throw new InvalidOperationException("AUTH_OTP_EXPIRED");
        if (storedOtp != request.Otp?.Trim())
            throw new InvalidOperationException("AUTH_OTP_INVALID");

        var (user, isNewUser) = await FindOrCreateUserAsync(phone, ct);
        if (user.IsBlocked)
            throw new InvalidOperationException("USER_BLOCKED");

        var accessToken = _jwt.GenerateAccessToken(user.Id, user.Role);
        var refreshToken = _jwt.GenerateRefreshToken(user.Id, user.Role);
        await _session.SetSessionAsync(user.Id, refreshToken, TimeSpan.FromDays(_options.RefreshExpiryDays), ct);
        await _userRepo.UpdateLastSeenAsync(user.Id, ct);

        return new VerifyOtpResult
        {
            AccessToken = accessToken,
            RefreshToken = refreshToken,
            TokenType = "Bearer",
            ExpiresIn = _jwt.GetAccessExpirySeconds(),
            User = new VerifyOtpUser
            {
                Id = user.Id,
                Phone = user.Phone,
                FullName = user.FullName,
                Role = user.Role,
                IsNewUser = isNewUser
            }
        };
    }

    /// <inheritdoc />
    public async Task<TokenResult> RefreshTokenAsync(RefreshTokenRequest request, CancellationToken ct = default)
    {
        var payload = _jwt.ValidateRefreshToken(request.RefreshToken ?? "");
        if (payload == null)
            throw new InvalidOperationException("AUTH_TOKEN_INVALID");

        var stored = await _session.GetSessionAsync(payload.Value.UserId, ct);
        if (stored == null || stored != request.RefreshToken)
            throw new InvalidOperationException("AUTH_TOKEN_INVALID");

        var user = await _userRepo.GetByIdAsync(payload.Value.UserId, ct);
        if (user == null || user.IsBlocked)
            throw new InvalidOperationException("USER_BLOCKED");

        var accessToken = _jwt.GenerateAccessToken(user.Id, user.Role);
        var refreshToken = _jwt.GenerateRefreshToken(user.Id, user.Role);
        await _session.SetSessionAsync(user.Id, refreshToken, TimeSpan.FromDays(_options.RefreshExpiryDays), ct);

        return new TokenResult
        {
            AccessToken = accessToken,
            RefreshToken = refreshToken,
            TokenType = "Bearer",
            ExpiresIn = _jwt.GetAccessExpirySeconds()
        };
    }

    /// <inheritdoc />
    public async Task LogoutAsync(Guid userId, CancellationToken ct = default)
    {
        await _session.DeleteSessionAsync(userId, ct);
    }

    private async Task<(User User, bool IsNewUser)> FindOrCreateUserAsync(string phone, CancellationToken ct)
    {
        var user = await _userRepo.GetByPhoneAsync(phone, ct);
        if (user != null)
            return (user, false);

        var newUser = new User
        {
            Phone = phone,
            PhoneVerified = true,
            Role = UserRole.Tenant,
            Language = UserLanguage.Uzbek,
            IsActive = true
        };
        var created = await _userRepo.CreateAsync(newUser, ct);
        return (created, true);
    }
}
