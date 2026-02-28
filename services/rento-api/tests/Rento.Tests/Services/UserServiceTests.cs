using Rento.Application.DTOs.Users;
using Rento.Application.Repositories;
using Rento.Application.Services.Implementations;
using Rento.Core.Entities;
using Moq;
using Xunit;

namespace Rento.Tests.Services;

/// <summary>
/// Unit tests for <see cref="UserService"/>.
/// </summary>
public sealed class UserServiceTests
{
    private readonly Mock<IUserRepository> _userRepo = new();

    [Fact]
    public async Task GetMeAsync_WhenUserExists_ReturnsProfile()
    {
        var userId = Guid.NewGuid();
        var user = new User
        {
            Id = userId,
            Phone = "+998901234567",
            FullName = "Test User",
            Role = "tenant",
            Language = "uz",
            CreatedAt = DateTime.UtcNow
        };
        _userRepo.Setup(r => r.GetByIdAsync(userId, It.IsAny<CancellationToken>())).ReturnsAsync(user);

        var sut = new UserService(_userRepo.Object);
        var result = await sut.GetMeAsync(userId);

        Assert.NotNull(result);
        Assert.Equal(userId, result.Id);
        Assert.Equal(user.Phone, result.Phone);
        Assert.Equal(user.FullName, result.FullName);
    }

    [Fact]
    public async Task GetMeAsync_WhenUserNotFound_ReturnsNull()
    {
        _userRepo.Setup(r => r.GetByIdAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync((User?)null);

        var sut = new UserService(_userRepo.Object);
        var result = await sut.GetMeAsync(Guid.NewGuid());

        Assert.Null(result);
    }

    [Fact]
    public async Task UpdateMeAsync_WhenAllFieldsEmpty_ReturnsNull()
    {
        var sut = new UserService(_userRepo.Object);
        var request = new UpdateProfileRequest { FullName = "", Email = "", Language = "" };

        var result = await sut.UpdateMeAsync(Guid.NewGuid(), request);

        Assert.Null(result);
        _userRepo.Verify(r => r.UpdateProfileAsync(It.IsAny<Guid>(), It.IsAny<string?>(), It.IsAny<string?>(), It.IsAny<string?>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task UpdateMeAsync_WhenFullNameProvided_UpdatesAndReturnsProfile()
    {
        var userId = Guid.NewGuid();
        var user = new User { Id = userId, Phone = "+998", FullName = "Updated", Role = "tenant", CreatedAt = DateTime.UtcNow };
        _userRepo.Setup(r => r.UpdateProfileAsync(userId, "New Name", null, null, It.IsAny<CancellationToken>())).ReturnsAsync(true);
        _userRepo.Setup(r => r.GetByIdAsync(userId, It.IsAny<CancellationToken>())).ReturnsAsync(user);

        var sut = new UserService(_userRepo.Object);
        var result = await sut.UpdateMeAsync(userId, new UpdateProfileRequest { FullName = "New Name" });

        Assert.NotNull(result);
    }
}
