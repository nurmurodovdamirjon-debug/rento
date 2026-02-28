using Minio;
using Minio.DataModel.Args;
using Rento.Application.Services;

namespace Rento.Infrastructure.Media;

public class MediaService : IMediaService
{
    private const long MaxFileSize = 5 * 1024 * 1024; // 5MB
    private static readonly string[] AllowedTypes = { "image/jpeg", "image/png", "image/webp" };
    private readonly IMinioClient _minio;
    private readonly string _baseUrl;
    private readonly string _bucketListings;
    private readonly string _bucketAvatars;

    public MediaService(IMinioClient minio, Microsoft.Extensions.Configuration.IConfiguration config)
    {
        _minio = minio;
        _baseUrl = config["MinIO:PublicUrl"] ?? "";
        _bucketListings = config["MinIO:BucketListings"] ?? "listings";
        _bucketAvatars = config["MinIO:BucketAvatars"] ?? "avatars";
    }

    public async Task<MediaUploadResult?> UploadAsync(Guid userId, Stream fileStream, string fileName, string contentType, string bucket, CancellationToken ct = default)
    {
        if (!AllowedTypes.Contains(contentType?.ToLowerInvariant()))
            return null;
        if (fileStream.Length > MaxFileSize)
            return null;
        if (bucket != "listings" && bucket != "avatars")
            return null;

        var bucketName = bucket == "avatars" ? _bucketAvatars : _bucketListings;
        var objectName = $"{userId}/{Guid.NewGuid():N}{Path.GetExtension(fileName)}";

        var putArgs = new PutObjectArgs()
            .WithBucket(bucketName)
            .WithObject(objectName)
            .WithStreamData(fileStream)
            .WithObjectSize(fileStream.Length)
            .WithContentType(contentType!);
        await _minio.PutObjectAsync(putArgs, ct).ConfigureAwait(false);

        var url = string.IsNullOrEmpty(_baseUrl) ? $"/{bucketName}/{objectName}" : $"{_baseUrl.TrimEnd('/')}/{bucketName}/{objectName}";
        var id = Guid.NewGuid();

        return new MediaUploadResult
        {
            Id = id,
            Url = url,
            ThumbnailUrl = null,
            SortOrder = 0,
            IsMain = false
        };
    }
}
