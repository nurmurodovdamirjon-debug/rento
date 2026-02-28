using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Application.Services.Implementations;
using Rento.Core.Common;
using Rento.Core.Entities;
using Moq;
using Xunit;

namespace Rento.Tests.Services;

/// <summary>
/// Unit tests for <see cref="ReportService"/>.
/// </summary>
public sealed class ReportServiceTests
{
    private readonly Mock<IReportRepository> _repo = new();

    [Fact]
    public async Task CreateAsync_WhenTargetTypeEmpty_ReturnsNull()
    {
        var sut = new ReportService(_repo.Object);
        var result = await sut.CreateAsync(Guid.NewGuid(), "", Guid.NewGuid(), "spam", null);

        Assert.Null(result);
        _repo.Verify(r => r.CreateAsync(It.IsAny<Report>(), It.IsAny<CancellationToken>()), Times.Never);
    }

    [Fact]
    public async Task CreateAsync_WhenAlreadyReported_ReturnsNull()
    {
        _repo.Setup(r => r.GetByReporterAndTargetAsync(It.IsAny<Guid>(), It.IsAny<string>(), It.IsAny<Guid>(), It.IsAny<CancellationToken>()))
            .ReturnsAsync(new Report { TargetType = "listing", Reason = "spam" });

        var sut = new ReportService(_repo.Object);
        var result = await sut.CreateAsync(Guid.NewGuid(), "listing", Guid.NewGuid(), "spam", null);

        Assert.Null(result);
    }

    [Fact]
    public async Task CreateAsync_WhenValid_CreatesAndReturnsDto()
    {
        var reporterId = Guid.NewGuid();
        var report = new Report
        {
            Id = Guid.NewGuid(),
            ReporterId = reporterId,
            TargetType = "listing",
            TargetId = Guid.NewGuid(),
            Reason = "spam",
            Status = ReportStatus.Pending,
            CreatedAt = DateTime.UtcNow
        };
        _repo.Setup(r => r.GetByReporterAndTargetAsync(reporterId, "listing", report.TargetId, It.IsAny<CancellationToken>())).ReturnsAsync((Report?)null);
        _repo.Setup(r => r.CreateAsync(It.IsAny<Report>(), It.IsAny<CancellationToken>())).ReturnsAsync(report);

        var sut = new ReportService(_repo.Object);
        var result = await sut.CreateAsync(reporterId, "listing", report.TargetId, "spam", null);

        Assert.NotNull(result);
        Assert.Equal(report.Id, result.Id);
        Assert.Equal("listing", result.TargetType);
        Assert.Equal("spam", result.Reason);
    }

    [Fact]
    public async Task GetListAsync_ReturnsPaginatedResult()
    {
        var items = new List<Report>();
        var paged = PaginatedResult<Report>.Create(items, 1, 20, 0);
        _repo.Setup(r => r.GetListAsync(null, 1, 20, It.IsAny<CancellationToken>())).ReturnsAsync(paged);

        var sut = new ReportService(_repo.Object);
        var result = await sut.GetListAsync(null, 1, 20);

        Assert.NotNull(result);
        Assert.Equal(0, result.Total);
    }

    [Fact]
    public async Task GetByIdAsync_WhenNotFound_ReturnsNull()
    {
        _repo.Setup(r => r.GetByIdAsync(It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync((Report?)null);

        var sut = new ReportService(_repo.Object);
        var result = await sut.GetByIdAsync(Guid.NewGuid());

        Assert.Null(result);
    }

    [Fact]
    public async Task ResolveAsync_DelegatesToRepository()
    {
        _repo.Setup(r => r.UpdateResolveAsync(It.IsAny<Guid>(), It.IsAny<string>(), It.IsAny<string?>(), It.IsAny<Guid>(), It.IsAny<CancellationToken>())).ReturnsAsync(true);

        var sut = new ReportService(_repo.Object);
        var ok = await sut.ResolveAsync(Guid.NewGuid(), ReportStatus.Resolved, "Done", Guid.NewGuid());

        Assert.True(ok);
    }
}
