using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Core.Common;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/notifications")]
[Authorize]
public class NotificationsController : ControllerBase
{
    [HttpGet]
    public IActionResult GetList([FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20)
    {
        return Ok(new ApiResponse { Success = true, Data = new { items = Array.Empty<object>(), meta = new { page, per_page = perPage, total = 0, total_pages = 0 } } });
    }

    [HttpGet("unread-count")]
    public IActionResult GetUnreadCount()
    {
        return Ok(new ApiResponse { Success = true, Data = new { unread_count = 0 } });
    }

    [HttpPut("{id}/read")]
    public IActionResult MarkRead(Guid id) => NoContent();

    [HttpPut("read-all")]
    public IActionResult MarkAllRead()
    {
        return Ok(new ApiResponse { Success = true, Data = new { marked_count = 0 } });
    }

    [HttpPost("fcm-token")]
    public IActionResult RegisterFcmToken([FromBody] FcmTokenRequest? body) => NoContent();

    [HttpDelete("fcm-token")]
    public IActionResult DeleteFcmToken([FromBody] FcmTokenRequest? body) => NoContent();
}

public class FcmTokenRequest
{
    public string? Token { get; set; }
    public string? DeviceType { get; set; }
}
