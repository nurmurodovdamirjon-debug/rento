using System.ComponentModel.DataAnnotations.Schema;

namespace Rento.Core.Entities;

[Table("fcm_tokens")]
public class FcmToken
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public string Token { get; set; } = "";
    public string DeviceType { get; set; } = "android";
    public bool IsActive { get; set; } = true;
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    public User User { get; set; } = null!;
}
