namespace Rento.Application.Services;

public interface IMediaService
{
    Task<MediaUploadResult?> UploadAsync(Guid userId, Stream fileStream, string fileName, string contentType, string bucket, CancellationToken ct = default);
}

public class MediaUploadResult
{
    public Guid Id { get; set; }
    public string Url { get; set; } = "";
    public string? ThumbnailUrl { get; set; }
    public short SortOrder { get; set; }
    public bool IsMain { get; set; }
}
