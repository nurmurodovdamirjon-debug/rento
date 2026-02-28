using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Application.Services;
using Rento.Core.Common;
using Rento.Core.Entities;

namespace Rento.Application.Services.Implementations;

public class ChatService(IChatRepository chatRepo, IListingRepository listingRepo) : IChatService
{

    public async Task<CreateOrGetRoomResult?> CreateOrGetRoomAsync(Guid listingId, Guid userId, string? initialMessage, CancellationToken ct = default)
    {
        var listing = await listingRepo.GetByIdWithImagesAsync(listingId, ct);
        if (listing == null) return null;
        var landlordId = listing.UserId;
        if (landlordId == userId) return null; // can't chat with self

        var existing = await chatRepo.GetByListingAndPartiesAsync(listingId, userId, landlordId, ct);
        if (existing != null)
        {
            if (!string.IsNullOrWhiteSpace(initialMessage))
            {
                var msg = new Message
                {
                    RoomId = existing.Id,
                    SenderId = userId,
                    Content = initialMessage,
                    MessageType = MessageType.Text
                };
                await chatRepo.AddMessageAsync(msg, ct);
                await chatRepo.UpdateRoomLastMessageAtAsync(existing.Id, DateTime.UtcNow, ct);
            }
            return new CreateOrGetRoomResult
            {
                RoomId = existing.Id,
                Listing = MapListing(existing.Listing),
                OtherUser = MapUser(existing.LandlordId == userId ? existing.Tenant : existing.Landlord),
                CreatedAt = existing.CreatedAt
            };
        }

        var room = new ChatRoom
        {
            ListingId = listingId,
            TenantId = userId,
            LandlordId = landlordId
        };
        var created = await chatRepo.CreateRoomAsync(room, ct);
        if (!string.IsNullOrWhiteSpace(initialMessage))
        {
            var msg = new Message
            {
                RoomId = created.Id,
                SenderId = userId,
                Content = initialMessage,
                MessageType = MessageType.Text
            };
            await chatRepo.AddMessageAsync(msg, ct);
            await chatRepo.UpdateRoomLastMessageAtAsync(created.Id, DateTime.UtcNow, ct);
        }

        created = await chatRepo.GetByIdAsync(created.Id, ct);
        if (created == null)
            return null;

        return new CreateOrGetRoomResult
        {
            RoomId = created.Id,
            Listing = MapListing(created.Listing),
            OtherUser = MapUser(created.Landlord),
            CreatedAt = created.CreatedAt
        };
    }

    public async Task<PaginatedResult<ChatRoomItemDto>> GetRoomsAsync(Guid userId, int page, int perPage, CancellationToken ct = default)
    {
        var result = await chatRepo.GetRoomsForUserAsync(userId, page, perPage, ct);
        var roomIds = result.Items.Select(r => r.Id).ToList();
        var lastMessages = await chatRepo.GetLastMessagesForRoomsAsync(roomIds, ct);

        var items = result.Items.Select(r =>
        {
            var other = r.TenantId == userId ? r.Landlord : r.Tenant;
            var lastMsg = lastMessages.GetValueOrDefault(r.Id);
            return new ChatRoomItemDto
            {
                RoomId = r.Id,
                Listing = MapListing(r.Listing),
                OtherUser = MapUser(other),
                LastMessage = lastMsg != null ? MapMessage(lastMsg) : null,
                LastMessageAt = r.LastMessageAt,
                CreatedAt = r.CreatedAt
            };
        }).ToList();

        return PaginatedResult<ChatRoomItemDto>.Create(items, result.Page, result.PerPage, result.Total);
    }

    public async Task<PaginatedResult<ChatMessageDto>?> GetMessagesAsync(Guid roomId, Guid userId, int page, int perPage, CancellationToken ct = default)
    {
        if (!await chatRepo.IsUserInRoomAsync(roomId, userId, ct)) return null;
        var result = await chatRepo.GetMessagesAsync(roomId, page, perPage, ct);
        return PaginatedResult<ChatMessageDto>.Create(
            result.Items.Select(MapMessage).ToList(),
            result.Page,
            result.PerPage,
            result.Total);
    }

    public async Task<ChatMessageDto?> SendMessageAsync(Guid roomId, Guid userId, string? content, string? messageType, string? mediaUrl, string? metadataJson, CancellationToken ct = default)
    {
        if (!await chatRepo.IsUserInRoomAsync(roomId, userId, ct)) return null;
        var msg = new Message
        {
            RoomId = roomId,
            SenderId = userId,
            Content = content,
            MessageType = messageType ?? MessageType.Text,
            MediaUrl = mediaUrl,
            Metadata = metadataJson
        };
        var added = await chatRepo.AddMessageAsync(msg, ct);
        await chatRepo.UpdateRoomLastMessageAtAsync(roomId, added.CreatedAt, ct);
        return MapMessage(added);
    }

    public async Task MarkReadAsync(Guid roomId, Guid userId, Guid? upToMessageId, CancellationToken ct = default)
    {
        if (!await chatRepo.IsUserInRoomAsync(roomId, userId, ct)) return;
        await chatRepo.MarkMessagesReadAsync(roomId, userId, upToMessageId, ct);
    }

    public async Task<bool> IsUserInRoomAsync(Guid roomId, Guid userId, CancellationToken ct = default)
        => await chatRepo.IsUserInRoomAsync(roomId, userId, ct);

    private static object MapListing(Listing? l)
    {
        if (l == null) return new { id = Guid.Empty, title = (string?)null, image_url = (string?)null };
        var images = l.ListingImages?.ToList() ?? [];
        var mainImg = images.FirstOrDefault(i => i.IsMain) ?? images.FirstOrDefault();
        return new
        {
            id = l.Id,
            title = l.Title,
            image_url = mainImg?.Url
        };
    }

    private static object MapUser(User? u)
    {
        if (u == null) return new { id = Guid.Empty, full_name = (string?)null, avatar_url = (string?)null };
        return new
        {
            id = u.Id,
            full_name = u.FullName,
            avatar_url = u.AvatarUrl
        };
    }

    private static ChatMessageDto MapMessage(Message m)
    {
        object? metadata = null;
        if (!string.IsNullOrWhiteSpace(m.Metadata))
        {
            try
            {
                metadata = System.Text.Json.JsonSerializer.Deserialize<object>(m.Metadata);
            }
            catch
            {
                // Invalid JSON: leave metadata null
            }
        }

        return new ChatMessageDto
        {
            Id = m.Id,
            RoomId = m.RoomId,
            SenderId = m.SenderId,
            Content = m.Content,
            MessageType = m.MessageType,
            MediaUrl = m.MediaUrl,
            Metadata = metadata,
            IsRead = m.IsRead,
            ReadAt = m.ReadAt,
            CreatedAt = m.CreatedAt
        };
    }
}
