using Minio;
using Minio.DataModel.Args;
using Microsoft.Extensions.Logging;
using Rento.Application.Services;
using Rento.Core.Common;

namespace Rento.Infrastructure.Media;

public class MediaService : IMediaService
{
    private const long MaxFileSize = 5 * 1024 * 1024; // 5MB

    private static readonly Dictionary<string, byte[]> MagicBytes = new()
    {
        ["image/jpeg"] = [0xFF, 0xD8, 0xFF],
        ["image/png"]  = [0x89, 0x50, 0x4E, 0x47],
        ["image/webp"] = [0x52, 0x49, 0x46, 0x46]
    };

    private static readonly Dictionary<string, string> AllowedExtensions = new()
    {
        ["image/jpeg"] = ".jpg",
        ["image/png"]  = ".png",
        ["image/webp"] = ".webp"
    };

    private readonly IMinioClient _minio;
    private readonly ILogger<MediaService> _logger;
    private readonly string _baseUrl;
    private readonly string _bucketListings;
    private readonly string _bucketAvatars;

    public MediaService(
        IMinioClient minio,
        ILogger<MediaService> logger,
        Microsoft.Extensions.Configuration.IConfiguration config)
    {
        _minio = minio;
        _logger = logger;
        _baseUrl = config["MinIO:PublicUrl"] ?? "";
        _bucketListings = config["MinIO:BucketListings"] ?? MediaBucket.Listings;
        _bucketAvatars = config["MinIO:BucketAvatars"] ?? MediaBucket.Avatars;
    }

    public async Task<MediaUploadResult?> UploadAsync(
        Guid userId,
        Stream fileStream,
        string fileName,
        string contentType,
        string bucket,
        CancellationToken ct = default)
    {
        var normalizedType = contentType?.ToLowerInvariant() ?? "";

        if (!AllowedExtensions.ContainsKey(normalizedType))
        {
            _logger.LogWarning("Upload rejected: unsupported content type {ContentType}", contentType);
            return null;
        }

        if (fileStream.Length > MaxFileSize)
        {
            _logger.LogWarning("Upload rejected: file size {Size} exceeds limit", fileStream.Length);
            return null;
        }

        if (bucket != MediaBucket.Listings && bucket != MediaBucket.Avatars)
        {
            _logger.LogWarning("Upload rejected: invalid bucket {Bucket}", bucket);
            return null;
        }

        if (!await ValidateMagicBytesAsync(fileStream, normalizedType, ct))
        {
            _logger.LogWarning("Upload rejected: magic bytes mismatch for content type {ContentType}", contentType);
            return null;
        }

        // Use safe extension from whitelist — never trust user-supplied filename
        var safeExtension = AllowedExtensions[normalizedType];
        var objectName = $"{userId}/{Guid.NewGuid():N}{safeExtension}";
        var bucketName = bucket == MediaBucket.Avatars ? _bucketAvatars : _bucketListings;

        try
        {
            var putArgs = new PutObjectArgs()
                .WithBucket(bucketName)
                .WithObject(objectName)
                .WithStreamData(fileStream)
                .WithObjectSize(fileStream.Length)
                .WithContentType(normalizedType);

            await _minio.PutObjectAsync(putArgs, ct).ConfigureAwait(false);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to upload object {ObjectName} to bucket {Bucket}", objectName, bucketName);
            return null;
        }

        var url = string.IsNullOrEmpty(_baseUrl)
            ? $"/{bucketName}/{objectName}"
            : $"{_baseUrl.TrimEnd('/')}/{bucketName}/{objectName}";

        return new MediaUploadResult
        {
            Id = Guid.NewGuid(),
            Url = url,
            ThumbnailUrl = null,
            SortOrder = 0,
            IsMain = false
        };
    }

    private static async Task<bool> ValidateMagicBytesAsync(Stream stream, string contentType, CancellationToken ct)
    {
        if (!MagicBytes.TryGetValue(contentType, out var expected))
            return false;

        var buffer = new byte[expected.Length];
        var read = await stream.ReadAsync(buffer.AsMemory(0, expected.Length), ct);
        stream.Position = 0;

        return read == expected.Length && buffer.SequenceEqual(expected);
    }
}
