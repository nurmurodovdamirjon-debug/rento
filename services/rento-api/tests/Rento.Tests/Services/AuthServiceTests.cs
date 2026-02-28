using Rento.Application.DTOs.Auth;
using Rento.Application.Options;
using Rento.Application.Repositories;
using Rento.Application.Services;
using Rento.Application.Services.Implementations;
using Rento.Core.Entities;
using Microsoft.Extensions.Options;
using Moq;
using Xunit;

namespace Rento.Tests.Services;

/// <summary>
/// Unit tests for <see cref="AuthService"/>.
/// </summary>
public sealed class AuthServiceTests
{
    private readonly Mock<IUserRepository> _userRepo = new();
    private readonly Mock<IJwtTokenService> _jwt = new();
    private readonly Mock<IOtpService> _otp = new();
    private readonly Mock<ISessionService> _session = new();
    private readonly Mock<ISmsService> _sms = new();
    private static readonly AuthOptions Options = new() { OtpExpirySeconds = 300, SmsMaxPerHour = 3, SmsWindowSeconds = 3600, RefreshExpiryDays = 7 };

    private static AuthService CreateSut() => new(
        new Mock<IUserRepository>().Object,
        new Mock<IJwtTokenService>().Object,
        new Mock<IOtpService>().Object,
        new Mock<ISessionService>().Object,
        new Mock<ISmsService>().Object,
        CreateOptions(Options));

    [Fact]
    public async Task SendOtpAsync_WhenOverLimit_ThrowsAuthOtpLimit()
    {
        _otp.Setup(r => r.GetAttemptsAsync(It.IsAny<string>(), It.IsAny<CancellationToken>())).ReturnsAsync(5);

        var sut = new AuthService(_userRepo.Object, _jwt.Object, _otp.Object, _session.Object, _sms.Object, CreateOptions(Options));

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            sut.SendOtpAsync(new SendOtpRequest { Phone = "+998901234567" }));
    }

    [Fact]
    public async Task SendOtpAsync_WhenUnderLimit_ReturnsResult()
    {
        _otp.Setup(r => r.GetAttemptsAsync(It.IsAny<string>(), It.IsAny<CancellationToken>())).ReturnsAsync(0);
        _otp.Setup(r => r.SetOtpAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<TimeSpan>(), It.IsAny<CancellationToken>())).Returns(Task.CompletedTask);
        _otp.Setup(r => r.IncrementAttemptsAsync(It.IsAny<string>(), It.IsAny<TimeSpan>(), It.IsAny<int>(), It.IsAny<CancellationToken>())).Returns(Task.CompletedTask);
        _otp.Setup(r => r.GetAttemptsRemainingAsync(It.IsAny<string>(), It.IsAny<int>(), It.IsAny<CancellationToken>())).ReturnsAsync(2);
        _sms.Setup(s => s.SendOtpAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<CancellationToken>())).Returns(Task.CompletedTask);

        var sut = new AuthService(_userRepo.Object, _jwt.Object, _otp.Object, _session.Object, _sms.Object, CreateOptions(Options));
        var result = await sut.SendOtpAsync(new SendOtpRequest { Phone = " +998901234567 " });

        Assert.Equal("+998901234567", result.Phone);
        Assert.Equal(300, result.ExpiresIn);
        Assert.Equal(2, result.AttemptsRemaining);
    }

    [Fact]
    public async Task VerifyOtpAsync_WhenOtpExpired_Throws()
    {
        _otp.Setup(r => r.GetAndDeleteOtpAsync(It.IsAny<string>(), It.IsAny<CancellationToken>())).ReturnsAsync((string?)null);

        var sut = new AuthService(_userRepo.Object, _jwt.Object, _otp.Object, _session.Object, _sms.Object, CreateOptions(Options));

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            sut.VerifyOtpAsync(new VerifyOtpRequest { Phone = "+998", Otp = "123456" }));
    }

    [Fact]
    public async Task VerifyOtpAsync_WhenOtpInvalid_Throws()
    {
        _otp.Setup(r => r.GetAndDeleteOtpAsync(It.IsAny<string>(), It.IsAny<CancellationToken>())).ReturnsAsync("999999");

        var sut = new AuthService(_userRepo.Object, _jwt.Object, _otp.Object, _session.Object, _sms.Object, CreateOptions(Options));

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            sut.VerifyOtpAsync(new VerifyOtpRequest { Phone = "+998", Otp = "123456" }));
    }

    [Fact]
    public async Task VerifyOtpAsync_WhenUserBlocked_Throws()
    {
        var user = new User { Id = Guid.NewGuid(), Phone = "+998", Role = "tenant", IsBlocked = true };
        _otp.Setup(r => r.GetAndDeleteOtpAsync(It.IsAny<string>(), It.IsAny<CancellationToken>())).ReturnsAsync("123456");
        _userRepo.Setup(r => r.GetByPhoneAsync(It.IsAny<string>(), It.IsAny<CancellationToken>())).ReturnsAsync(user);

        var sut = new AuthService(_userRepo.Object, _jwt.Object, _otp.Object, _session.Object, _sms.Object, CreateOptions(Options));

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            sut.VerifyOtpAsync(new VerifyOtpRequest { Phone = "+998", Otp = "123456" }));
    }

    [Fact]
    public async Task LogoutAsync_DelegatesToSession()
    {
        var userId = Guid.NewGuid();
        _session.Setup(s => s.DeleteSessionAsync(userId, It.IsAny<CancellationToken>())).Returns(Task.CompletedTask);

        var sut = new AuthService(_userRepo.Object, _jwt.Object, _otp.Object, _session.Object, _sms.Object, CreateOptions(Options));
        await sut.LogoutAsync(userId);

        _session.Verify(s => s.DeleteSessionAsync(userId, It.IsAny<CancellationToken>()), Times.Once);
    }

    private static IOptions<AuthOptions> CreateOptions(AuthOptions value)
    {
        var mock = new Mock<IOptions<AuthOptions>>();
        mock.Setup(m => m.Value).Returns(value);
        return mock.Object;
    }
}
