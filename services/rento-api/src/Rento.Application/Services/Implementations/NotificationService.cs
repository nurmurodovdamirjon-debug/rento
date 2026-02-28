using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Application.Services;
using Rento.Core.Common;
using Rento.Core.Entities;

namespace Rento.Application.Services.Implementations;

public class NotificationService(INotificationRepository repo) : INotificationService
{
    public async Task<PaginatedResult<NotificationItemDto>> GetListAsync(Guid userId, int page, int perPage, CancellationToken ct = default)
    {
        var result = await repo.GetByUserIdAsync(userId, page, perPage, ct);
        return new PaginatedResult<NotificationItemDto>
        {
            Items = result.Items.Select(Map).ToList(),
            Page = result.Page,
            PerPage = result.PerPage,
            Total = result.Total,
            TotalPages = result.TotalPages
        };
    }

    public Task<int> GetUnreadCountAsync(Guid userId, CancellationToken ct = default)
        => repo.GetUnreadCountAsync(userId, ct);

    public Task<bool> MarkReadAsync(Guid id, Guid userId, CancellationToken ct = default)
        => repo.MarkReadAsync(id, userId, ct);

    public Task<int> MarkAllReadAsync(Guid userId, CancellationToken ct = default)
        => repo.MarkAllReadAsync(userId, ct);

    public Task RegisterFcmTokenAsync(Guid userId, string token, string? deviceType, CancellationToken ct = default)
    {
        return repo.UpsertFcmTokenAsync(userId, token, deviceType ?? DeviceType.Android, ct);
    }

    public Task DeleteFcmTokenAsync(Guid userId, string token, CancellationToken ct = default)
        => repo.DeleteFcmTokenAsync(userId, token, ct);

    private static NotificationItemDto Map(Notification n) => new()
    {
        Id = n.Id,
        Type = n.Type,
        Title = n.Title,
        Body = n.Body,
        RefType = n.RefType,
        RefId = n.RefId,
        IsRead = n.IsRead,
        ReadAt = n.ReadAt,
        CreatedAt = n.CreatedAt
    };
}
