using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Rento.Application.Services;

namespace Rento.Infrastructure.Sms;

public class EskizSmsService : ISmsService
{
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly IConfiguration _config;
    private readonly ILogger<EskizSmsService> _logger;
    private readonly string _env;

    public EskizSmsService(IHttpClientFactory httpClientFactory, IConfiguration config, ILogger<EskizSmsService> logger)
    {
        _httpClientFactory = httpClientFactory;
        _config = config;
        _logger = logger;
        _env = config["App:Env"] ?? config["APP_ENV"] ?? "Development";
    }

    public async Task SendOtpAsync(string phone, string otp, CancellationToken ct = default)
    {
        if (_env is "Development" or "Test")
        {
            _logger.LogInformation("[DEV SMS] Phone: {Phone}, OTP: {Otp}", phone, otp);
            return;
        }

        var baseUrl = _config["Eskiz:BaseUrl"]?.TrimEnd('/') ?? "https://notify.eskiz.uz/api";
        var email = _config["Eskiz:Email"];
        var password = _config["Eskiz:Password"];
        if (string.IsNullOrEmpty(email) || string.IsNullOrEmpty(password))
        {
            _logger.LogWarning("Eskiz credentials not set; skipping SMS");
            return;
        }

        var client = _httpClientFactory.CreateClient();
        try
        {
            var authResponse = await client.PostAsJsonAsync($"{baseUrl}/auth/login",
                new { email, password }, ct);
            authResponse.EnsureSuccessStatusCode();
            var authJson = await authResponse.Content.ReadFromJsonAsync<JsonElement>(cancellationToken: ct);
            var token = authJson.GetProperty("data").GetProperty("token").GetString();
            if (string.IsNullOrEmpty(token))
                throw new InvalidOperationException("Eskiz auth returned no token");

            var sendBody = new
            {
                mobile_phone = phone.TrimStart('+'),
                message = $"Rento tasdiqlash kodi: {otp}. 5 daqiqa ichida kiriting.",
                from = "4546"
            };
            var request = new HttpRequestMessage(HttpMethod.Post, $"{baseUrl}/message/sms/send");
            request.Headers.Add("Authorization", "Bearer " + token);
            request.Content = JsonContent.Create(sendBody);
            var sendResponse = await client.SendAsync(request, ct);
            sendResponse.EnsureSuccessStatusCode();
            _logger.LogInformation("SMS sent to {Phone}", phone[..Math.Min(7, phone.Length)] + "****");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "SMS send failed for {Phone}", phone);
            throw;
        }
    }
}
