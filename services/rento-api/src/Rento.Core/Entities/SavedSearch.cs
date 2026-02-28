using System.ComponentModel.DataAnnotations.Schema;

namespace Rento.Core.Entities;

[Table("saved_searches")]
public class SavedSearch
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public required string Name { get; set; }
    public string? City { get; set; }
    public string? District { get; set; }
    public string? Type { get; set; }
    public decimal? MinPrice { get; set; }
    public decimal? MaxPrice { get; set; }
    public short? Rooms { get; set; }
    public bool NotifyEnabled { get; set; } = true;
    public DateTime? LastNotified { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    public User User { get; set; } = null!;
}
