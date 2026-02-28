using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Application.Services;
using Rento.Core.Common;
using Rento.Core.Entities;

namespace Rento.Application.Services.Implementations;

public class ReportService(IReportRepository repo) : IReportService
{
    public async Task<ReportItemDto?> CreateAsync(Guid reporterId, string targetType, Guid targetId, string reason, string? description, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(targetType) || string.IsNullOrWhiteSpace(reason))
            return null;
        var existing = await repo.GetByReporterAndTargetAsync(reporterId, targetType, targetId, ct);
        if (existing != null)
            return null; // AlreadyReported - caller can check and return error
        var report = new Report
        {
            ReporterId = reporterId,
            TargetType = targetType,
            TargetId = targetId,
            Reason = reason,
            Description = description ?? ""
        };
        var created = await repo.CreateAsync(report, ct);
        return Map(created);
    }

    public async Task<PaginatedResult<ReportItemDto>> GetListAsync(string? status, int page, int perPage, CancellationToken ct = default)
    {
        var result = await repo.GetListAsync(status, page, perPage, ct);
        return new PaginatedResult<ReportItemDto>
        {
            Items = result.Items.Select(Map).ToList(),
            Page = result.Page,
            PerPage = result.PerPage,
            Total = result.Total,
            TotalPages = result.TotalPages
        };
    }

    public async Task<ReportItemDto?> GetByIdAsync(Guid id, CancellationToken ct = default)
    {
        var r = await repo.GetByIdAsync(id, ct);
        return r == null ? null : Map(r);
    }

    public Task<bool> ResolveAsync(Guid id, string status, string? adminNote, Guid resolvedBy, CancellationToken ct = default)
        => repo.UpdateResolveAsync(id, status ?? ReportStatus.Resolved, adminNote, resolvedBy, ct);

    private static ReportItemDto Map(Report r) => new()
    {
        Id = r.Id,
        ReporterId = r.ReporterId,
        TargetType = r.TargetType,
        TargetId = r.TargetId,
        Reason = r.Reason,
        Description = r.Description,
        Status = r.Status,
        AdminNote = r.AdminNote,
        ResolvedBy = r.ResolvedBy,
        ResolvedAt = r.ResolvedAt,
        CreatedAt = r.CreatedAt,
        Reporter = r.Reporter != null ? new { id = r.Reporter.Id, full_name = r.Reporter.FullName, phone = r.Reporter.Phone } : null
    };
}
