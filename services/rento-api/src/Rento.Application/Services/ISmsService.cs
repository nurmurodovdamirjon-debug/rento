namespace Rento.Application.Services;

public interface ISmsService
{
    Task SendOtpAsync(string phone, string otp, CancellationToken ct = default);
}
