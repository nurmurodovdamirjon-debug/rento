using Rento.Application.Common;
using Rento.Core.Entities;

namespace Rento.Application.Repositories;

public interface INotificationRepository
{
    Task<PaginatedResult<Notification>> GetByUserIdAsync(Guid userId, int page, int perPage, CancellationToken ct = default);
    Task<int> GetUnreadCountAsync(Guid userId, CancellationToken ct = default);
    Task<Notification?> GetByIdAndUserIdAsync(Guid id, Guid userId, CancellationToken ct = default);
    Task<int> MarkAllReadAsync(Guid userId, CancellationToken ct = default);
    Task<bool> MarkReadAsync(Guid id, Guid userId, CancellationToken ct = default);
    Task<FcmToken?> GetFcmTokenAsync(Guid userId, string token, CancellationToken ct = default);
    Task<FcmToken> UpsertFcmTokenAsync(Guid userId, string token, string deviceType, CancellationToken ct = default);
    Task<bool> DeleteFcmTokenAsync(Guid userId, string token, CancellationToken ct = default);
    Task<Notification> CreateAsync(Notification notification, CancellationToken ct = default);
}
