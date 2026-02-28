using Rento.Application.Common;

namespace Rento.Application.Services;

public class NotificationItemDto
{
    public Guid Id { get; set; }
    public string Type { get; set; } = "";
    public string Title { get; set; } = "";
    public string Body { get; set; } = "";
    public string? RefType { get; set; }
    public Guid? RefId { get; set; }
    public bool IsRead { get; set; }
    public DateTime? ReadAt { get; set; }
    public DateTime CreatedAt { get; set; }
}

public interface INotificationService
{
    Task<PaginatedResult<NotificationItemDto>> GetListAsync(Guid userId, int page, int perPage, CancellationToken ct = default);
    Task<int> GetUnreadCountAsync(Guid userId, CancellationToken ct = default);
    Task<bool> MarkReadAsync(Guid id, Guid userId, CancellationToken ct = default);
    Task<int> MarkAllReadAsync(Guid userId, CancellationToken ct = default);
    Task RegisterFcmTokenAsync(Guid userId, string token, string? deviceType, CancellationToken ct = default);
    Task DeleteFcmTokenAsync(Guid userId, string token, CancellationToken ct = default);
}
