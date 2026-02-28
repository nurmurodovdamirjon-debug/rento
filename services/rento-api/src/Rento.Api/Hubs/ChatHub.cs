using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;

namespace Rento.Api.Hubs;

[Authorize]
public class ChatHub : Hub
{
    public async Task JoinRoom(string roomId)
    {
        await Groups.AddToGroupAsync(Context.ConnectionId, roomId);
    }

    public async Task LeaveRoom(string roomId)
    {
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, roomId);
    }

    public async Task SendMessage(string roomId, string? content, string? type, string? mediaUrl, object? metadata)
    {
        var userId = Context.User?.FindFirst("sub")?.Value ?? Context.User?.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        await Clients.Group(roomId).SendAsync("NewMessage", new
        {
            id = Guid.NewGuid(),
            room_id = roomId,
            sender_id = userId,
            content,
            type = type ?? "text",
            media_url = mediaUrl,
            metadata,
            created_at = DateTime.UtcNow
        });
    }

    public async Task TypingStart(string roomId)
    {
        var userId = Context.User?.FindFirst("sub")?.Value ?? Context.User?.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        await Clients.OthersInGroup(roomId).SendAsync("UserTyping", new { room_id = roomId, user_id = userId });
    }

    public async Task TypingStop(string roomId)
    {
        var userId = Context.User?.FindFirst("sub")?.Value ?? Context.User?.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        await Clients.OthersInGroup(roomId).SendAsync("UserStopTyping", new { room_id = roomId, user_id = userId });
    }

    public async Task MarkRead(string roomId, string messageId)
    {
        var userId = Context.User?.FindFirst("sub")?.Value ?? Context.User?.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        await Clients.OthersInGroup(roomId).SendAsync("MessageRead", new { room_id = roomId, reader_id = userId, last_read_id = messageId });
    }

    public override async Task OnConnectedAsync()
    {
        var userId = Context.User?.FindFirst("sub")?.Value ?? Context.User?.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        await Clients.Others.SendAsync("UserOnline", new { user_id = userId });
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        var userId = Context.User?.FindFirst("sub")?.Value ?? Context.User?.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
        await Clients.Others.SendAsync("UserOffline", new { user_id = userId });
        await base.OnDisconnectedAsync(exception);
    }
}
