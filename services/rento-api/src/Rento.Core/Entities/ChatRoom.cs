using System.ComponentModel.DataAnnotations.Schema;

namespace Rento.Core.Entities;

[Table("chat_rooms")]
public class ChatRoom
{
    public Guid Id { get; set; }
    public Guid ListingId { get; set; }
    public Guid TenantId { get; set; }
    public Guid LandlordId { get; set; }
    public DateTime? LastMessageAt { get; set; }
    public bool IsActive { get; set; } = true;
    public DateTime CreatedAt { get; set; }

    public Listing Listing { get; set; } = null!;
    public User Tenant { get; set; } = null!;
    public User Landlord { get; set; } = null!;
    public ICollection<Message> Messages { get; set; } = new List<Message>();
}
