using Rento.Application.Common;
using Rento.Core.Entities;

namespace Rento.Application.Repositories;

public interface IReportRepository
{
    Task<Report?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<Report?> GetByReporterAndTargetAsync(Guid reporterId, string targetType, Guid targetId, CancellationToken ct = default);
    Task<PaginatedResult<Report>> GetListAsync(string? status, int page, int perPage, CancellationToken ct = default);
    Task<Report> CreateAsync(Report report, CancellationToken ct = default);
    Task<bool> UpdateResolveAsync(Guid id, string status, string? adminNote, Guid resolvedBy, CancellationToken ct = default);
}
