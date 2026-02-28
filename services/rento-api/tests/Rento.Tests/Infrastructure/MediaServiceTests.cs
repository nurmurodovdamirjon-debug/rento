using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Minio;
using Moq;
using Rento.Core.Common;
using Rento.Infrastructure.Media;
using Xunit;

namespace Rento.Tests.Infrastructure;

public sealed class MediaServiceTests
{
    private readonly Mock<IMinioClient> _minio = new();
    private readonly Mock<ILogger<MediaService>> _logger = new();

    [Fact]
    public async Task UploadAsync_WhenContentTypeUnsupported_ReturnsNull()
    {
        using var stream = new MemoryStream([1, 2, 3, 4]);
        var sut = CreateSut();

        var result = await sut.UploadAsync(Guid.NewGuid(), stream, "test.bin", "application/octet-stream", MediaBucket.Listings);

        Assert.Null(result);
    }

    [Fact]
    public async Task UploadAsync_WhenBucketInvalid_ReturnsNull()
    {
        using var stream = new MemoryStream([0xFF, 0xD8, 0xFF, 0xAA]);
        var sut = CreateSut();

        var result = await sut.UploadAsync(Guid.NewGuid(), stream, "test.jpg", "image/jpeg", "invalid-bucket");

        Assert.Null(result);
    }

    [Fact]
    public async Task UploadAsync_WhenFileTooLarge_ReturnsNull()
    {
        using var stream = new MemoryStream(new byte[(5 * 1024 * 1024) + 1]);
        var sut = CreateSut();

        var result = await sut.UploadAsync(Guid.NewGuid(), stream, "large.jpg", "image/jpeg", MediaBucket.Listings);

        Assert.Null(result);
    }

    private MediaService CreateSut()
    {
        var config = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["MinIO:PublicUrl"] = "http://localhost:9000",
                ["MinIO:BucketListings"] = MediaBucket.Listings,
                ["MinIO:BucketAvatars"] = MediaBucket.Avatars
            })
            .Build();

        return new MediaService(_minio.Object, _logger.Object, config);
    }
}
