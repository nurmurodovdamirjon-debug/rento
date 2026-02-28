using System.ComponentModel.DataAnnotations.Schema;
using Rento.Core.Common;

namespace Rento.Core.Entities;

[Table("messages")]
public class Message
{
    public Guid Id { get; set; }
    public Guid RoomId { get; set; }
    public Guid SenderId { get; set; }
    public string? Content { get; set; }
    public string MessageType { get; set; } = Rento.Core.Common.MessageType.Text;
    public string? MediaUrl { get; set; }
    public string? Metadata { get; set; }
    public bool IsRead { get; set; }
    public DateTime? ReadAt { get; set; }
    public DateTime CreatedAt { get; set; }

    public ChatRoom Room { get; set; } = null!;
    public User Sender { get; set; } = null!;
}
