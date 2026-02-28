using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Application.Services.Implementations;
using Rento.Core.Entities;
using Moq;
using Xunit;

namespace Rento.Tests.Services;

/// <summary>
/// Unit tests for <see cref="FavoriteService"/>.
/// </summary>
public sealed class FavoriteServiceTests
{
    private readonly Mock<IFavoriteRepository> _repo = new();

    [Fact]
    public async Task ToggleAsync_WhenNotFavorite_AddsAndReturnsTrue()
    {
        var userId = Guid.NewGuid();
        var listingId = Guid.NewGuid();
        _repo.Setup(r => r.ToggleAsync(userId, listingId, It.IsAny<CancellationToken>())).ReturnsAsync(true);

        var sut = new FavoriteService(_repo.Object);
        var result = await sut.ToggleAsync(userId, listingId);

        Assert.True(result.IsFavorite);
    }

    [Fact]
    public async Task ToggleAsync_WhenAlreadyFavorite_RemovesAndReturnsFalse()
    {
        _repo.Setup(r => r.ToggleAsync(It.IsAny<Guid>(), It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync(false);

        var sut = new FavoriteService(_repo.Object);
        var result = await sut.ToggleAsync(Guid.NewGuid(), Guid.NewGuid());

        Assert.False(result.IsFavorite);
    }

    [Fact]
    public async Task GetListAsync_ReturnsPaginatedResult()
    {
        var userId = Guid.NewGuid();
        var items = new List<Favorite>();
        _repo.Setup(r => r.GetPageAsync(userId, 1, 20, It.IsAny<CancellationToken>())).ReturnsAsync((items, 0));

        var sut = new FavoriteService(_repo.Object);
        var result = await sut.GetListAsync(userId, 1, 20);

        Assert.NotNull(result);
        Assert.Equal(1, result.Page);
        Assert.Equal(20, result.PerPage);
        Assert.Equal(0, result.Total);
    }

    [Fact]
    public async Task CheckAsync_DelegatesToRepository()
    {
        _repo.Setup(r => r.ExistsAsync(It.IsAny<Guid>(), It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync(true);

        var sut = new FavoriteService(_repo.Object);
        var result = await sut.CheckAsync(Guid.NewGuid(), Guid.NewGuid());

        Assert.True(result);
    }

    [Fact]
    public async Task GetIdsAsync_DelegatesToRepository()
    {
        var ids = new List<Guid> { Guid.NewGuid() };
        _repo.Setup(r => r.GetListingIdsAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync(ids);

        var sut = new FavoriteService(_repo.Object);
        var result = await sut.GetIdsAsync(Guid.NewGuid());

        Assert.Single(result);
        Assert.Equal(ids[0], result[0]);
    }
}
