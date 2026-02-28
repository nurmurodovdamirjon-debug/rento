using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using Rento.Application.Services;
using Rento.Api.Extensions;
using Rento.Core.Common;

namespace Rento.Api.Hubs;

[Authorize]
public class ChatHub : Hub
{
    private readonly IChatService _chatService;

    public ChatHub(IChatService chatService)
    {
        _chatService = chatService;
    }

    public async Task JoinRoom(string roomId)
    {
        var userId = Context.User?.GetUserId();
        if (userId == null)
        {
            await Clients.Caller.SendAsync("Error", new { message = "Unauthorized" });
            return;
        }
        if (!Guid.TryParse(roomId, out var roomGuid))
        {
            await Clients.Caller.SendAsync("Error", new { message = "Invalid room ID" });
            return;
        }
        if (!await _chatService.IsUserInRoomAsync(roomGuid, userId.Value, Context.ConnectionAborted))
        {
            await Clients.Caller.SendAsync("Error", new { message = "Access denied" });
            return;
        }
        await Groups.AddToGroupAsync(Context.ConnectionId, roomId);
    }

    public async Task LeaveRoom(string roomId)
    {
        var userId = Context.User?.GetUserId();
        if (userId == null) return;
        if (!Guid.TryParse(roomId, out var roomGuid)) return;
        if (!await _chatService.IsUserInRoomAsync(roomGuid, userId.Value, Context.ConnectionAborted)) return;
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, roomId);
    }

    public async Task SendMessage(string roomId, string? content, string? type, string? mediaUrl, object? metadata)
    {
        var userId = Context.User?.GetUserId();
        if (userId == null)
        {
            await Clients.Caller.SendAsync("Error", new { message = "Unauthorized" });
            return;
        }
        if (!Guid.TryParse(roomId, out var roomGuid))
        {
            await Clients.Caller.SendAsync("Error", new { message = "Invalid room ID" });
            return;
        }
        var metadataJson = metadata != null ? System.Text.Json.JsonSerializer.Serialize(metadata) : null;
        var msg = await _chatService.SendMessageAsync(roomGuid, userId.Value, content, type ?? MessageType.Text, mediaUrl, metadataJson, Context.ConnectionAborted);
        if (msg == null)
        {
            await Clients.Caller.SendAsync("Error", new { message = "Room not found or access denied" });
            return;
        }
        await Clients.Group(roomId).SendAsync("NewMessage", new
        {
            id = msg.Id,
            room_id = msg.RoomId,
            sender_id = msg.SenderId,
            content = msg.Content,
            type = msg.MessageType,
            media_url = msg.MediaUrl,
            metadata = msg.Metadata,
            created_at = msg.CreatedAt
        });
    }

    public async Task TypingStart(string roomId)
    {
        var userId = Context.User?.GetUserId();
        if (userId == null) return;
        await Clients.OthersInGroup(roomId).SendAsync("UserTyping", new { room_id = roomId, user_id = userId });
    }

    public async Task TypingStop(string roomId)
    {
        var userId = Context.User?.GetUserId();
        if (userId == null) return;
        await Clients.OthersInGroup(roomId).SendAsync("UserStopTyping", new { room_id = roomId, user_id = userId });
    }

    public async Task MarkRead(string roomId, string messageId)
    {
        var userId = Context.User?.GetUserId();
        if (userId == null) return;
        if (!Guid.TryParse(roomId, out var roomGuid)) return;
        Guid? upTo = Guid.TryParse(messageId, out var mid) ? mid : null;
        await _chatService.MarkReadAsync(roomGuid, userId.Value, upTo, Context.ConnectionAborted);
        await Clients.OthersInGroup(roomId).SendAsync("MessageRead", new { room_id = roomId, reader_id = userId, last_read_id = messageId });
    }

    public override async Task OnConnectedAsync()
    {
        var userId = Context.User?.GetUserId();
        if (userId != null)
            await Clients.Others.SendAsync("UserOnline", new { user_id = userId });
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        var userId = Context.User?.GetUserId();
        if (userId != null)
            await Clients.Others.SendAsync("UserOffline", new { user_id = userId });
        await base.OnDisconnectedAsync(exception);
    }
}
