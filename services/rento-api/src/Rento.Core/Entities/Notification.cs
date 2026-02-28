using System.ComponentModel.DataAnnotations.Schema;

namespace Rento.Core.Entities;

[Table("notifications")]
public class Notification
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public required string Type { get; set; }
    public required string Title { get; set; }
    public required string Body { get; set; }
    public string? RefType { get; set; }
    public Guid? RefId { get; set; }
    public bool IsRead { get; set; }
    public DateTime? ReadAt { get; set; }
    public bool PushSent { get; set; }
    public DateTime? PushSentAt { get; set; }
    public string? Metadata { get; set; }
    public DateTime CreatedAt { get; set; }

    public User User { get; set; } = null!;
}
