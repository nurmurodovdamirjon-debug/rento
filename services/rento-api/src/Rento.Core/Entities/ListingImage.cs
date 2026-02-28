using System.ComponentModel.DataAnnotations.Schema;

namespace Rento.Core.Entities;

[Table("listing_images")]
public class ListingImage
{
    public Guid Id { get; set; }
    public Guid ListingId { get; set; }
    public required string Url { get; set; }
    public string? ThumbnailUrl { get; set; }
    public short SortOrder { get; set; }
    public bool IsMain { get; set; }
    public DateTime CreatedAt { get; set; }

    public Listing Listing { get; set; } = null!;
}
