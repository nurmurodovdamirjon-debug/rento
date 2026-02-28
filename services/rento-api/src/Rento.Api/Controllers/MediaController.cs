using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Api.Extensions;
using Rento.Application.Services;
using Rento.Core.Common;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/media")]
[Authorize]
public class MediaController : ControllerBase
{
    private readonly IMediaService _mediaService;

    public MediaController(IMediaService mediaService)
    {
        _mediaService = mediaService;
    }

    [HttpPost("upload")]
    [RequestSizeLimit(5 * 1024 * 1024)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> Upload(IFormFile? file, [FromForm] string? bucket, CancellationToken ct)
    {
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });

        if (file == null || file.Length == 0)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.FileRequired, Message = "File is required" } });

        var b = (bucket ?? MediaBucket.Listings).ToLowerInvariant();
        if (b != MediaBucket.Listings && b != MediaBucket.Avatars)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.InvalidBucket, Message = "Bucket must be listings or avatars" } });

        if (file.Length > 5 * 1024 * 1024)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.MediaTooLarge, Message = "Max size 5MB" } });

        var contentType = file.ContentType?.ToLowerInvariant() ?? "";
        if (contentType != "image/jpeg" && contentType != "image/png" && contentType != "image/webp")
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.MediaInvalidType, Message = "Allowed: JPEG, PNG, WebP" } });

        await using var stream = file.OpenReadStream();
        var result = await _mediaService.UploadAsync(userId.Value, stream, file.FileName, file.ContentType ?? "application/octet-stream", b, ct);
        if (result == null)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.MediaInvalidType, Message = "Invalid file" } });

        return Ok(new ApiResponse { Success = true, Data = result });
    }
}
