using Rento.Application.Common;
using Rento.Application.DTOs.Listings;
using Rento.Application.Repositories;
using Rento.Application.Services.Implementations;
using Rento.Core.Common;
using Rento.Core.Entities;
using Moq;
using Xunit;

namespace Rento.Tests.Services;

/// <summary>
/// Unit tests for <see cref="ListingService"/>.
/// </summary>
public sealed class ListingServiceTests
{
    private readonly Mock<IListingRepository> _listingRepo = new();

    [Fact]
    public async Task GetListingsAsync_ReturnsPaginatedResult()
    {
        var filter = new ListingFilter { Page = 1, PerPage = 20 };
        var items = new List<Listing>();
        var paged = PaginatedResult<Listing>.Create(items, 1, 20, 0);
        _listingRepo.Setup(r => r.GetListingsAsync(filter, It.IsAny<CancellationToken>())).ReturnsAsync(paged);

        var sut = new ListingService(_listingRepo.Object);
        var result = await sut.GetListingsAsync(filter);

        Assert.NotNull(result);
        Assert.Equal(0, result.Total);
    }

    [Fact]
    public async Task GetByIdAsync_WhenNotFound_ReturnsNull()
    {
        _listingRepo.Setup(r => r.GetByIdWithImagesAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync((Listing?)null);

        var sut = new ListingService(_listingRepo.Object);
        var result = await sut.GetByIdAsync(Guid.NewGuid(), null);

        Assert.Null(result);
    }

    [Fact]
    public async Task GetNearbyAsync_ReturnsEmptyPaginatedResult()
    {
        var filter = new NearbyFilter { Page = 1, PerPage = 20 };
        var sut = new ListingService(_listingRepo.Object);

        var result = await sut.GetNearbyAsync(filter);

        Assert.NotNull(result);
        Assert.Empty(result.Items);
        Assert.Equal(0, result.Total);
    }

    [Fact]
    public async Task DeleteAsync_DelegatesToRepository()
    {
        _listingRepo.Setup(r => r.DeleteAsync(It.IsAny<Guid>(), It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync(true);

        var sut = new ListingService(_listingRepo.Object);
        var ok = await sut.DeleteAsync(Guid.NewGuid(), Guid.NewGuid());

        Assert.True(ok);
    }

    [Fact]
    public async Task UpdateStatusAsync_DelegatesToRepository()
    {
        _listingRepo.Setup(r => r.UpdateStatusAsync(It.IsAny<Guid>(), It.IsAny<Guid>(), It.IsAny<string>(), It.IsAny<CancellationToken>())).ReturnsAsync(true);

        var sut = new ListingService(_listingRepo.Object);
        var ok = await sut.UpdateStatusAsync(Guid.NewGuid(), Guid.NewGuid(), ListingStatus.Active);

        Assert.True(ok);
    }
}
