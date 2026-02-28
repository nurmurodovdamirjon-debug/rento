using Rento.Application.Common;

namespace Rento.Application.Services;

/// <summary>
/// Chat room and message DTO for listing conversation.
/// </summary>
public class ChatRoomItemDto
{
    public Guid RoomId { get; init; }
    public object? Listing { get; init; }
    public object? OtherUser { get; init; }
    public object? LastMessage { get; init; }
    public DateTime? LastMessageAt { get; init; }
    public DateTime CreatedAt { get; init; }
}

/// <summary>
/// Single chat message DTO.
/// </summary>
public class ChatMessageDto
{
    public Guid Id { get; init; }
    public Guid RoomId { get; init; }
    public Guid SenderId { get; init; }
    public string? Content { get; init; }
    public string MessageType { get; init; } = Rento.Core.Common.MessageType.Text;
    public string? MediaUrl { get; init; }
    public object? Metadata { get; init; }
    public bool IsRead { get; init; }
    public DateTime? ReadAt { get; init; }
    public DateTime CreatedAt { get; init; }
}

/// <summary>
/// Result of create-or-get chat room.
/// </summary>
public class CreateOrGetRoomResult
{
    public Guid RoomId { get; init; }
    public object? Listing { get; init; }
    public object? OtherUser { get; init; }
    public DateTime CreatedAt { get; init; }
}

/// <summary>
/// Chat rooms and messages service.
/// </summary>
/// <summary>
/// Chat rooms and messages service.
/// </summary>
public interface IChatService
{
    /// <summary>Creates a chat room for listing or returns existing; adds initial message if provided. Returns null if listing not found or user is landlord.</summary>
    Task<CreateOrGetRoomResult?> CreateOrGetRoomAsync(Guid listingId, Guid userId, string? initialMessage, CancellationToken ct = default);

    /// <summary>Paginated list of chat rooms for the user.</summary>
    Task<PaginatedResult<ChatRoomItemDto>> GetRoomsAsync(Guid userId, int page, int perPage, CancellationToken ct = default);

    /// <summary>Paginated messages in a room; returns null if user is not a participant.</summary>
    Task<PaginatedResult<ChatMessageDto>?> GetMessagesAsync(Guid roomId, Guid userId, int page, int perPage, CancellationToken ct = default);

    /// <summary>Sends a message in the room; returns null if user not in room.</summary>
    Task<ChatMessageDto?> SendMessageAsync(Guid roomId, Guid userId, string? content, string? messageType, string? mediaUrl, string? metadataJson, CancellationToken ct = default);

    /// <summary>Marks messages as read up to optional message id.</summary>
    Task MarkReadAsync(Guid roomId, Guid userId, Guid? upToMessageId, CancellationToken ct = default);

    /// <summary>Checks if the user is a participant of the room.</summary>
    Task<bool> IsUserInRoomAsync(Guid roomId, Guid userId, CancellationToken ct = default);
}
