using Microsoft.EntityFrameworkCore;
using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Core.Entities;
using Rento.Infrastructure.Data;

namespace Rento.Infrastructure.Repositories;

public class ChatRepository(RentoDbContext db) : IChatRepository
{

    public async Task<ChatRoom?> GetByIdAsync(Guid roomId, CancellationToken ct = default)
    {
        return await db.ChatRooms
            .AsNoTracking()
            .Include(r => r.Listing).ThenInclude(l => l.ListingImages.Where(i => i.IsMain))
            .Include(r => r.Tenant)
            .Include(r => r.Landlord)
            .FirstOrDefaultAsync(r => r.Id == roomId, ct);
    }

    public async Task<ChatRoom?> GetByListingAndPartiesAsync(Guid listingId, Guid tenantId, Guid landlordId, CancellationToken ct = default)
    {
        return await db.ChatRooms
            .AsNoTracking()
            .Include(r => r.Listing).ThenInclude(l => l.ListingImages.Where(i => i.IsMain))
            .Include(r => r.Tenant)
            .Include(r => r.Landlord)
            .FirstOrDefaultAsync(r => r.ListingId == listingId && r.TenantId == tenantId && r.LandlordId == landlordId && r.IsActive, ct);
    }

    public async Task<PaginatedResult<ChatRoom>> GetRoomsForUserAsync(Guid userId, int page, int perPage, CancellationToken ct = default)
    {
        var q = db.ChatRooms
            .AsNoTracking()
            .Where(r => r.IsActive && (r.TenantId == userId || r.LandlordId == userId))
            .OrderByDescending(r => r.LastMessageAt ?? r.CreatedAt);

        var total = await q.CountAsync(ct);

        // Include before Skip/Take to avoid loading all rows into memory
        var items = await q
            .Include(r => r.Listing).ThenInclude(l => l.ListingImages.Where(i => i.IsMain))
            .Include(r => r.Tenant)
            .Include(r => r.Landlord)
            .Skip((page - 1) * perPage)
            .Take(perPage)
            .ToListAsync(ct);

        return PaginatedResult<ChatRoom>.Create(items, page, perPage, total);
    }

    public async Task<IReadOnlyDictionary<Guid, Message>> GetLastMessagesForRoomsAsync(IReadOnlyList<Guid> roomIds, CancellationToken ct = default)
    {
        if (roomIds.Count == 0)
            return new Dictionary<Guid, Message>();

        var roomIdsArray = roomIds.Distinct().ToArray();

        // Efficient: subquery gets latest message ID per room, then fetch with sender
        var latestMessageIds = db.Messages
            .Where(m => roomIdsArray.Contains(m.RoomId))
            .GroupBy(m => m.RoomId)
            .Select(g => g.OrderByDescending(m => m.CreatedAt).First().Id);

        var messages = await db.Messages
            .AsNoTracking()
            .Include(m => m.Sender)
            .Where(m => latestMessageIds.Contains(m.Id))
            .ToListAsync(ct);

        return messages.ToDictionary(m => m.RoomId);
    }

    public async Task<PaginatedResult<Message>> GetMessagesAsync(Guid roomId, int page, int perPage, CancellationToken ct = default)
    {
        var q = db.Messages
            .AsNoTracking()
            .Where(m => m.RoomId == roomId)
            .OrderByDescending(m => m.CreatedAt);

        var total = await q.CountAsync(ct);

        // Include before Skip/Take
        var items = await q
            .Include(m => m.Sender)
            .Skip((page - 1) * perPage)
            .Take(perPage)
            .ToListAsync(ct);

        return PaginatedResult<Message>.Create(items, page, perPage, total);
    }

    public async Task<ChatRoom> CreateRoomAsync(ChatRoom room, CancellationToken ct = default)
    {
        room.Id = Guid.NewGuid();
        room.CreatedAt = DateTime.UtcNow;
        room.IsActive = true;
        db.ChatRooms.Add(room);
        await db.SaveChangesAsync(ct);
        return room;
    }

    public async Task<Message> AddMessageAsync(Message message, CancellationToken ct = default)
    {
        message.Id = Guid.NewGuid();
        message.CreatedAt = DateTime.UtcNow;
        db.Messages.Add(message);
        await db.SaveChangesAsync(ct);
        return message;
    }

    public async Task UpdateRoomLastMessageAtAsync(Guid roomId, DateTime at, CancellationToken ct = default)
    {
        await db.ChatRooms
            .Where(r => r.Id == roomId)
            .ExecuteUpdateAsync(s => s.SetProperty(r => r.LastMessageAt, at), ct);
    }

    public async Task MarkMessagesReadAsync(Guid roomId, Guid readerId, Guid? upToMessageId, CancellationToken ct = default)
    {
        var query = db.Messages
            .Where(m => m.RoomId == roomId && m.SenderId != readerId && !m.IsRead);
        if (upToMessageId.HasValue)
        {
            var msgDate = await db.Messages
                .Where(m => m.Id == upToMessageId.Value)
                .Select(m => m.CreatedAt)
                .FirstOrDefaultAsync(ct);
            query = query.Where(m => m.CreatedAt <= msgDate);
        }
        await query.ExecuteUpdateAsync(s => s
            .SetProperty(m => m.IsRead, true)
            .SetProperty(m => m.ReadAt, DateTime.UtcNow), ct);
    }

    public async Task<bool> IsUserInRoomAsync(Guid roomId, Guid userId, CancellationToken ct = default)
    {
        return await db.ChatRooms
            .AnyAsync(r => r.Id == roomId && (r.TenantId == userId || r.LandlordId == userId), ct);
    }
}
