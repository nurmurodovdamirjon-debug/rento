using Rento.Application.Common;
using Xunit;

namespace Rento.Tests.Common;

/// <summary>
/// Unit tests for <see cref="PaginatedResult{T}"/>.
/// </summary>
public sealed class PaginatedResultTests
{
    [Fact]
    public void Create_WithValidInputs_ReturnsCorrectTotalPages()
    {
        var items = new List<int> { 1, 2, 3 };
        var result = PaginatedResult<int>.Create(items, page: 1, perPage: 10, total: 25);

        Assert.Equal(3, result.Items.Count);
        Assert.Equal(1, result.Page);
        Assert.Equal(10, result.PerPage);
        Assert.Equal(25, result.Total);
        Assert.Equal(3, result.TotalPages);
    }

    [Fact]
    public void Create_WithZeroPerPage_TotalPagesIsZero()
    {
        var result = PaginatedResult<string>.Create([], page: 1, perPage: 0, total: 100);

        Assert.Equal(0, result.TotalPages);
    }

    [Fact]
    public void Create_WithEmptyItems_StillComputesTotalPages()
    {
        var result = PaginatedResult<bool>.Create([], page: 2, perPage: 20, total: 50);

        Assert.Empty(result.Items);
        Assert.Equal(2, result.Page);
        Assert.Equal(50, result.Total);
        Assert.Equal(3, result.TotalPages);
    }
}
