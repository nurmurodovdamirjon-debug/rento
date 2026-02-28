using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Application.Services;
using Rento.Core.Common;
using Rento.Api.Extensions;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/notifications")]
[Authorize]
public class NotificationsController : ControllerBase
{
    private readonly INotificationService _notificationService;

    public NotificationsController(INotificationService notificationService)
    {
        _notificationService = notificationService;
    }

    [HttpGet]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> GetList([FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20, CancellationToken ct = default)
    {
        var userId = User.GetUserId();
        if (userId == null) return Unauthorized();
        var result = await _notificationService.GetListAsync(userId.Value, page, perPage, ct);
        return Ok(new ApiResponse { Success = true, Data = new { items = result.Items, meta = new { result.Page, per_page = result.PerPage, total = result.Total, total_pages = result.TotalPages } } });
    }

    [HttpGet("unread-count")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> GetUnreadCount(CancellationToken ct = default)
    {
        var userId = User.GetUserId();
        if (userId == null) return Unauthorized();
        var count = await _notificationService.GetUnreadCountAsync(userId.Value, ct);
        return Ok(new ApiResponse { Success = true, Data = new { unread_count = count } });
    }

    [HttpPut("{id}/read")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> MarkRead(Guid id, CancellationToken ct = default)
    {
        var userId = User.GetUserId();
        if (userId == null) return Unauthorized();
        var found = await _notificationService.MarkReadAsync(id, userId.Value, ct);
        if (!found) return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.NotFound, Message = "Notification not found" } });
        return NoContent();
    }

    [HttpPut("read-all")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> MarkAllRead(CancellationToken ct = default)
    {
        var userId = User.GetUserId();
        if (userId == null) return Unauthorized();
        var markedCount = await _notificationService.MarkAllReadAsync(userId.Value, ct);
        return Ok(new ApiResponse { Success = true, Data = new { marked_count = markedCount } });
    }

    [HttpPost("fcm-token")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> RegisterFcmToken([FromBody] FcmTokenRequest? body, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(body?.Token))
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "token required" } });
        var userId = User.GetUserId();
        if (userId == null) return Unauthorized();
        await _notificationService.RegisterFcmTokenAsync(userId.Value, body.Token, body.DeviceType, ct);
        return NoContent();
    }

    [HttpDelete("fcm-token")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> DeleteFcmToken([FromBody] FcmTokenRequest? body, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(body?.Token))
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "token required" } });
        var userId = User.GetUserId();
        if (userId == null) return Unauthorized();
        await _notificationService.DeleteFcmTokenAsync(userId.Value, body.Token, ct);
        return NoContent();
    }
}

public class FcmTokenRequest
{
    public string? Token { get; init; }
    public string? DeviceType { get; init; }
}
