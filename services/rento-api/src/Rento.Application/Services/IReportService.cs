using Rento.Application.Common;

namespace Rento.Application.Services;

public class ReportItemDto
{
    public Guid Id { get; set; }
    public Guid ReporterId { get; set; }
    public string TargetType { get; set; } = "";
    public Guid TargetId { get; set; }
    public string Reason { get; set; } = "";
    public string? Description { get; set; }
    public string Status { get; set; } = "";
    public string? AdminNote { get; set; }
    public Guid? ResolvedBy { get; set; }
    public DateTime? ResolvedAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public object? Reporter { get; set; }
}

public interface IReportService
{
    Task<ReportItemDto?> CreateAsync(Guid reporterId, string targetType, Guid targetId, string reason, string? description, CancellationToken ct = default);
    Task<PaginatedResult<ReportItemDto>> GetListAsync(string? status, int page, int perPage, CancellationToken ct = default);
    Task<ReportItemDto?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<bool> ResolveAsync(Guid id, string status, string? adminNote, Guid resolvedBy, CancellationToken ct = default);
}
