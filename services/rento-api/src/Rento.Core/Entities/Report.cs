using System.ComponentModel.DataAnnotations.Schema;
using Rento.Core.Common;

namespace Rento.Core.Entities;

[Table("reports")]
public class Report
{
    public Guid Id { get; set; }
    public Guid ReporterId { get; set; }
    public required string TargetType { get; set; }
    public Guid TargetId { get; set; }
    public required string Reason { get; set; }
    public string? Description { get; set; }
    public string Status { get; set; } = ReportStatus.Pending;
    public string? AdminNote { get; set; }
    public Guid? ResolvedBy { get; set; }
    public DateTime? ResolvedAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    public User Reporter { get; set; } = null!;
}
