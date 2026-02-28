using Rento.Application.Common;
using Rento.Core.Entities;

namespace Rento.Application.Repositories;

public interface IChatRepository
{
    Task<ChatRoom?> GetByIdAsync(Guid roomId, CancellationToken ct = default);
    Task<ChatRoom?> GetByListingAndPartiesAsync(Guid listingId, Guid tenantId, Guid landlordId, CancellationToken ct = default);
    Task<PaginatedResult<ChatRoom>> GetRoomsForUserAsync(Guid userId, int page, int perPage, CancellationToken ct = default);
    Task<IReadOnlyDictionary<Guid, Message>> GetLastMessagesForRoomsAsync(IReadOnlyList<Guid> roomIds, CancellationToken ct = default);
    Task<PaginatedResult<Message>> GetMessagesAsync(Guid roomId, int page, int perPage, CancellationToken ct = default);
    Task<ChatRoom> CreateRoomAsync(ChatRoom room, CancellationToken ct = default);
    Task<Message> AddMessageAsync(Message message, CancellationToken ct = default);
    Task UpdateRoomLastMessageAtAsync(Guid roomId, DateTime at, CancellationToken ct = default);
    Task MarkMessagesReadAsync(Guid roomId, Guid readerId, Guid? upToMessageId, CancellationToken ct = default);
    Task<bool> IsUserInRoomAsync(Guid roomId, Guid userId, CancellationToken ct = default);
}
