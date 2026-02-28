using Microsoft.EntityFrameworkCore;
using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Core.Common;
using Rento.Core.Entities;
using Rento.Infrastructure.Data;

namespace Rento.Infrastructure.Repositories;

public class NotificationRepository(RentoDbContext db) : INotificationRepository
{

    public async Task<PaginatedResult<Notification>> GetByUserIdAsync(Guid userId, int page, int perPage, CancellationToken ct = default)
    {
        var q = db.Notifications.AsNoTracking().Where(n => n.UserId == userId).OrderByDescending(n => n.CreatedAt);
        var total = await q.CountAsync(ct);
        var items = await q.Skip((page - 1) * perPage).Take(perPage).ToListAsync(ct);
        return PaginatedResult<Notification>.Create(items, page, perPage, total);
    }

    public async Task<int> GetUnreadCountAsync(Guid userId, CancellationToken ct = default)
        => await db.Notifications.CountAsync(n => n.UserId == userId && !n.IsRead, ct);

    public async Task<Notification?> GetByIdAndUserIdAsync(Guid id, Guid userId, CancellationToken ct = default)
        => await db.Notifications.AsNoTracking().FirstOrDefaultAsync(n => n.Id == id && n.UserId == userId, ct);

    public async Task<int> MarkAllReadAsync(Guid userId, CancellationToken ct = default)
    {
        var updated = await db.Notifications
            .Where(n => n.UserId == userId && !n.IsRead)
            .ExecuteUpdateAsync(s => s
                .SetProperty(n => n.IsRead, true)
                .SetProperty(n => n.ReadAt, DateTime.UtcNow), ct);
        return updated;
    }

    public async Task<bool> MarkReadAsync(Guid id, Guid userId, CancellationToken ct = default)
    {
        var updated = await db.Notifications
            .Where(n => n.Id == id && n.UserId == userId)
            .ExecuteUpdateAsync(s => s
                .SetProperty(n => n.IsRead, true)
                .SetProperty(n => n.ReadAt, DateTime.UtcNow), ct);
        return updated > 0;
    }

    public async Task<FcmToken?> GetFcmTokenAsync(Guid userId, string token, CancellationToken ct = default)
        => await db.FcmTokens.AsNoTracking().FirstOrDefaultAsync(f => f.UserId == userId && f.Token == token, ct);

    public async Task<FcmToken> UpsertFcmTokenAsync(Guid userId, string token, string deviceType, CancellationToken ct = default)
    {
        // Track for update; AsNoTracking not used here intentionally
        var existing = await db.FcmTokens.FirstOrDefaultAsync(f => f.UserId == userId && f.Token == token, ct);
        var now = DateTime.UtcNow;
        if (existing != null)
        {
            existing.DeviceType = string.IsNullOrWhiteSpace(deviceType) ? DeviceType.Android : deviceType;
            existing.IsActive = true;
            existing.UpdatedAt = now;
        }
        else
        {
            existing = new FcmToken
            {
                Id = Guid.NewGuid(),
                UserId = userId,
                Token = token,
                DeviceType = string.IsNullOrWhiteSpace(deviceType) ? DeviceType.Android : deviceType,
                IsActive = true,
                CreatedAt = now,
                UpdatedAt = now
            };
            db.FcmTokens.Add(existing);
        }
        // Single SaveChangesAsync for both insert and update branches
        await db.SaveChangesAsync(ct);
        return existing;
    }

    public async Task<bool> DeleteFcmTokenAsync(Guid userId, string token, CancellationToken ct = default)
    {
        var deleted = await db.FcmTokens
            .Where(f => f.UserId == userId && f.Token == token)
            .ExecuteDeleteAsync(ct);
        return deleted > 0;
    }

    public async Task<Notification> CreateAsync(Notification notification, CancellationToken ct = default)
    {
        notification.Id = Guid.NewGuid();
        notification.CreatedAt = DateTime.UtcNow;
        db.Notifications.Add(notification);
        await db.SaveChangesAsync(ct);
        return notification;
    }
}
