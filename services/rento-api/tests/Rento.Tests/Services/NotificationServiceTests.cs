using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Application.Services.Implementations;
using Rento.Core.Common;
using Rento.Core.Entities;
using Moq;
using Xunit;

namespace Rento.Tests.Services;

/// <summary>
/// Unit tests for <see cref="NotificationService"/>.
/// </summary>
public sealed class NotificationServiceTests
{
    private readonly Mock<INotificationRepository> _repo = new();

    [Fact]
    public async Task GetListAsync_ReturnsPaginatedResult()
    {
        var userId = Guid.NewGuid();
        var items = new List<Notification> { new() { Id = Guid.NewGuid(), UserId = userId, Type = "info", Title = "Test", Body = "Body", CreatedAt = DateTime.UtcNow } };
        var paged = PaginatedResult<Notification>.Create(items, 1, 20, 1);
        _repo.Setup(r => r.GetByUserIdAsync(userId, 1, 20, It.IsAny<CancellationToken>())).ReturnsAsync(paged);

        var sut = new NotificationService(_repo.Object);
        var result = await sut.GetListAsync(userId, 1, 20);

        Assert.Single(result.Items);
        Assert.Equal(1, result.Total);
    }

    [Fact]
    public async Task GetUnreadCountAsync_DelegatesToRepository()
    {
        _repo.Setup(r => r.GetUnreadCountAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync(5);

        var sut = new NotificationService(_repo.Object);
        var count = await sut.GetUnreadCountAsync(Guid.NewGuid());

        Assert.Equal(5, count);
    }

    [Fact]
    public async Task MarkReadAsync_DelegatesToRepository()
    {
        _repo.Setup(r => r.MarkReadAsync(It.IsAny<Guid>(), It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync(true);

        var sut = new NotificationService(_repo.Object);
        var found = await sut.MarkReadAsync(Guid.NewGuid(), Guid.NewGuid());

        Assert.True(found);
    }

    [Fact]
    public async Task MarkAllReadAsync_DelegatesToRepository()
    {
        _repo.Setup(r => r.MarkAllReadAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync(3);

        var sut = new NotificationService(_repo.Object);
        var count = await sut.MarkAllReadAsync(Guid.NewGuid());

        Assert.Equal(3, count);
    }

    [Fact]
    public async Task RegisterFcmTokenAsync_DelegatesToRepository()
    {
        var token = new FcmToken { Id = Guid.NewGuid(), UserId = Guid.NewGuid(), Token = "t", DeviceType = DeviceType.Android };
        _repo.Setup(r => r.UpsertFcmTokenAsync(It.IsAny<Guid>(), It.IsAny<string>(), It.IsAny<string>(), It.IsAny<CancellationToken>())).ReturnsAsync(token);

        var sut = new NotificationService(_repo.Object);
        await sut.RegisterFcmTokenAsync(Guid.NewGuid(), "token", DeviceType.Android);

        _repo.Verify(r => r.UpsertFcmTokenAsync(It.IsAny<Guid>(), "token", DeviceType.Android, It.IsAny<CancellationToken>()), Times.Once);
    }
}
