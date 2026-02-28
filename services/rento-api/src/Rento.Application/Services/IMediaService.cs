namespace Rento.Application.Services;

public interface IMediaService
{
    Task<MediaUploadResult?> UploadAsync(Guid userId, Stream fileStream, string fileName, string contentType, string bucket, CancellationToken ct = default);
}

public class MediaUploadResult
{
    public Guid Id { get; init; }
    public required string Url { get; init; }
    public string? ThumbnailUrl { get; init; }
    public short SortOrder { get; init; }
    public bool IsMain { get; init; }
}
