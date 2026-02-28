using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Application.Services;
using Rento.Core.Common;
using Rento.Api.Extensions;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/chats")]
[Authorize]
public class ChatsController : ControllerBase
{
    private readonly IChatService _chatService;

    public ChatsController(IChatService chatService)
    {
        _chatService = chatService;
    }

    [HttpPost]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> CreateOrGet([FromBody] CreateChatRequest? body, CancellationToken ct)
    {
        if (body?.ListingId == null)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "listing_id required" } });
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized();
        var result = await _chatService.CreateOrGetRoomAsync(body.ListingId.Value, userId.Value, body.InitialMessage, ct);
        if (result == null)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "Listing not found or cannot chat with self" } });
        return StatusCode(201, new ApiResponse { Success = true, Data = new { room_id = result.RoomId, listing = result.Listing, other_user = result.OtherUser, created_at = result.CreatedAt } });
    }

    [HttpGet]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> GetList([FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20, CancellationToken ct = default)
    {
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized();
        var result = await _chatService.GetRoomsAsync(userId.Value, page, perPage, ct);
        return Ok(new ApiResponse { Success = true, Data = new { items = result.Items, meta = new { result.Page, per_page = result.PerPage, total = result.Total, total_pages = result.TotalPages } } });
    }

    [HttpGet("{roomId}/messages")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetMessages(Guid roomId, [FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20, CancellationToken ct = default)
    {
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized();
        var result = await _chatService.GetMessagesAsync(roomId, userId.Value, page, perPage, ct);
        if (result == null)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.NotFound, Message = "Room not found or access denied" } });
        return Ok(new ApiResponse { Success = true, Data = new { items = result.Items, meta = new { result.Page, per_page = result.PerPage, total = result.Total, total_pages = result.TotalPages } } });
    }

    [HttpPost("{roomId}/messages")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> SendMessage(Guid roomId, [FromBody] SendMessageRequest? body, CancellationToken ct)
    {
        if (body == null)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "content required for text" } });
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized();
        var msg = await _chatService.SendMessageAsync(roomId, userId.Value, body.Content, body.MessageType, body.MediaUrl, body.Metadata != null ? System.Text.Json.JsonSerializer.Serialize(body.Metadata) : null, ct);
        if (msg == null)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.NotFound, Message = "Room not found or access denied" } });
        return StatusCode(201, new ApiResponse { Success = true, Data = new { msg.Id, room_id = msg.RoomId, sender_id = msg.SenderId, content = msg.Content, type = msg.MessageType, media_url = msg.MediaUrl, metadata = msg.Metadata, created_at = msg.CreatedAt } });
    }

    [HttpPut("{roomId}/read")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> MarkRead(Guid roomId, [FromQuery] Guid? message_id, CancellationToken ct)
    {
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized();
        await _chatService.MarkReadAsync(roomId, userId.Value, message_id, ct);
        return NoContent();
    }
}

public class CreateChatRequest
{
    public Guid? ListingId { get; init; }
    public string? InitialMessage { get; init; }
}

public class SendMessageRequest
{
    public string? Content { get; init; }
    public string? MessageType { get; init; }
    public string? MediaUrl { get; init; }
    public object? Metadata { get; init; }
}
