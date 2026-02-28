namespace Rento.Application.Options;

/// <summary>
/// Authentication and OTP configuration options.
/// </summary>
public sealed class AuthOptions
{
    public const string SectionName = "Auth";

    /// <summary>OTP expiry in seconds.</summary>
    public int OtpExpirySeconds { get; set; } = 300;

    /// <summary>Max SMS send attempts per phone per window.</summary>
    public int SmsMaxPerHour { get; set; } = 3;

    /// <summary>SMS rate limit window in seconds.</summary>
    public int SmsWindowSeconds { get; set; } = 3600;

    /// <summary>Refresh token validity in days.</summary>
    public int RefreshExpiryDays { get; set; } = 7;
}
