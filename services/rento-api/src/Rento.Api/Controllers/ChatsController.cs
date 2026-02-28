using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Core.Common;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/chats")]
[Authorize]
public class ChatsController : ControllerBase
{
    [HttpPost]
    public IActionResult CreateOrGet([FromBody] CreateChatRequest? body, CancellationToken ct)
    {
        if (body == null)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "listing_id and initial_message required" } });
        return StatusCode(201, new ApiResponse { Success = true, Data = new { room_id = Guid.NewGuid(), listing = new { id = body.ListingId, title = "", image_url = (string?)null }, other_user = (object?)null, created_at = DateTime.UtcNow } });
    }

    [HttpGet]
    public IActionResult GetList([FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20)
    {
        return Ok(new ApiResponse { Success = true, Data = new { items = Array.Empty<object>(), meta = new { page, per_page = perPage, total = 0, total_pages = 0 } } });
    }

    [HttpGet("{roomId}/messages")]
    public IActionResult GetMessages(Guid roomId, [FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20)
    {
        return Ok(new ApiResponse { Success = true, Data = new { items = Array.Empty<object>(), meta = new { page, per_page = perPage, total = 0, total_pages = 0 } } });
    }

    [HttpPost("{roomId}/messages")]
    public IActionResult SendMessage(Guid roomId, [FromBody] SendMessageRequest? body, CancellationToken ct)
    {
        if (body == null)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "content required for text" } });
        return StatusCode(201, new ApiResponse { Success = true, Data = new { id = Guid.NewGuid(), room_id = roomId, sender_id = GetUserId(), content = body.Content, type = body.MessageType ?? "text", created_at = DateTime.UtcNow } });
    }

    [HttpPut("{roomId}/read")]
    public IActionResult MarkRead(Guid roomId) => NoContent();

    private Guid? GetUserId()
    {
        var sub = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub");
        return Guid.TryParse(sub, out var id) ? id : null;
    }
}

public class CreateChatRequest
{
    public Guid? ListingId { get; set; }
    public string? InitialMessage { get; set; }
}

public class SendMessageRequest
{
    public string? Content { get; set; }
    public string? MessageType { get; set; }
    public string? MediaUrl { get; set; }
    public object? Metadata { get; set; }
}
