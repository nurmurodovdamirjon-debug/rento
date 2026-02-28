using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
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

    private Guid? GetUserId()
    {
        var sub = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub");
        return Guid.TryParse(sub, out var id) ? id : null;
    }

    [HttpPost("upload")]
    [RequestSizeLimit(5 * 1024 * 1024)]
    public async Task<IActionResult> Upload(IFormFile? file, [FromForm] string? bucket, CancellationToken ct)
    {
        var userId = GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });

        if (file == null || file.Length == 0)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.FileRequired, Message = "File is required" } });

        var b = (bucket ?? "listings").ToLowerInvariant();
        if (b != "listings" && b != "avatars")
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
