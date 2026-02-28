namespace Rento.Application.Services;

public interface IOtpService
{
    Task SetOtpAsync(string phone, string otp, TimeSpan ttl, CancellationToken ct = default);
    Task<string?> GetAndDeleteOtpAsync(string phone, CancellationToken ct = default);
    Task<int> GetAttemptsAsync(string phone, CancellationToken ct = default);
    Task IncrementAttemptsAsync(string phone, TimeSpan window, int maxAttempts, CancellationToken ct = default);
    Task<int> GetAttemptsRemainingAsync(string phone, int maxAttempts, CancellationToken ct = default);
}
