using Microsoft.EntityFrameworkCore;
using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Core.Common;
using Rento.Core.Entities;
using Rento.Infrastructure.Data;

namespace Rento.Infrastructure.Repositories;

public class ReportRepository(RentoDbContext db) : IReportRepository
{

    public async Task<Report?> GetByIdAsync(Guid id, CancellationToken ct = default)
        => await db.Reports.AsNoTracking().Include(r => r.Reporter).FirstOrDefaultAsync(r => r.Id == id, ct);

    public async Task<Report?> GetByReporterAndTargetAsync(Guid reporterId, string targetType, Guid targetId, CancellationToken ct = default)
        => await db.Reports.AsNoTracking().FirstOrDefaultAsync(r => r.ReporterId == reporterId && r.TargetType == targetType && r.TargetId == targetId, ct);

    public async Task<PaginatedResult<Report>> GetListAsync(string? status, int page, int perPage, CancellationToken ct = default)
    {
        IQueryable<Report> q = db.Reports.AsNoTracking().Include(r => r.Reporter);
        if (!string.IsNullOrWhiteSpace(status))
            q = q.Where(r => r.Status == status);
        q = q.OrderByDescending(r => r.CreatedAt);
        var total = await q.CountAsync(ct);
        var items = await q.Skip((page - 1) * perPage).Take(perPage).ToListAsync(ct);
        return PaginatedResult<Report>.Create(items, page, perPage, total);
    }

    public async Task<Report> CreateAsync(Report report, CancellationToken ct = default)
    {
        report.Id = Guid.NewGuid();
        report.CreatedAt = DateTime.UtcNow;
        report.UpdatedAt = DateTime.UtcNow;
        report.Status = ReportStatus.Pending;
        db.Reports.Add(report);
        await db.SaveChangesAsync(ct);
        return report;
    }

    public async Task<bool> UpdateResolveAsync(Guid id, string status, string? adminNote, Guid resolvedBy, CancellationToken ct = default)
    {
        var updated = await db.Reports
            .Where(r => r.Id == id)
            .ExecuteUpdateAsync(s => s
                .SetProperty(r => r.Status, status)
                .SetProperty(r => r.AdminNote, adminNote ?? "")
                .SetProperty(r => r.ResolvedBy, resolvedBy)
                .SetProperty(r => r.ResolvedAt, DateTime.UtcNow)
                .SetProperty(r => r.UpdatedAt, DateTime.UtcNow), ct);
        return updated > 0;
    }
}
