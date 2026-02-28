using System.ComponentModel.DataAnnotations.Schema;
using Rento.Core.Common;

namespace Rento.Core.Entities;

[Table("users")]
public class User
{
    public Guid Id { get; set; }
    public required string Phone { get; set; }
    public bool PhoneVerified { get; set; }
    public string? FullName { get; set; }
    public string? Email { get; set; }
    public string? AvatarUrl { get; set; }
    public string Role { get; set; } = UserRole.Tenant;
    public bool IdVerified { get; set; }
    public string? IdDocumentUrl { get; set; }
    public DateTime? IdVerifiedAt { get; set; }
    public decimal RatingAvg { get; set; }
    public int RatingCount { get; set; }
    public string Subscription { get; set; } = UserSubscription.Free;
    public DateTime? SubExpiresAt { get; set; }
    public string Language { get; set; } = UserLanguage.Uzbek;
    public DateTime? LastSeenAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public bool IsActive { get; set; } = true;
    public bool IsBlocked { get; set; }

    public ICollection<Listing> Listings { get; set; } = [];
    public ICollection<Favorite> Favorites { get; set; } = [];
    public ICollection<Notification> Notifications { get; set; } = [];
    public ICollection<FcmToken> FcmTokens { get; set; } = [];
    public ICollection<Report> ReportsAsReporter { get; set; } = [];
}
