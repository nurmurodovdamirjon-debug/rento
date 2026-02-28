using System.ComponentModel.DataAnnotations.Schema;
using Rento.Core.Common;

namespace Rento.Core.Entities;

[Table("listings")]
public class Listing
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public required string Type { get; set; }
    public string DealType { get; set; } = ListingDealType.Rent;
    public required string City { get; set; }
    public string? District { get; set; }
    public string? Address { get; set; }
    public string? Landmark { get; set; }
    public decimal? Latitude { get; set; }
    public decimal? Longitude { get; set; }
    public short? Rooms { get; set; }
    public short? Floor { get; set; }
    public short? TotalFloors { get; set; }
    public decimal? AreaSqm { get; set; }
    public decimal Price { get; set; }
    public string Currency { get; set; } = Rento.Core.Common.Currency.Uzs;
    public bool PriceNegotiable { get; set; }
    public bool HasFurniture { get; set; }
    public bool HasAppliances { get; set; }
    public bool HasInternet { get; set; }
    public bool HasParking { get; set; }
    public bool HasConditioner { get; set; }
    public bool AllowsPets { get; set; }
    public bool AllowsChildren { get; set; } = true;
    public bool UtilitiesIncluded { get; set; }
    public decimal? DepositAmount { get; set; }
    public string Status { get; set; } = ListingStatus.Pending;
    public string? RejectionReason { get; set; }
    public bool IsPremium { get; set; }
    public DateTime? PremiumUntil { get; set; }
    public int ViewsCount { get; set; }
    public int FavoritesCount { get; set; }
    public int ContactsCount { get; set; }
    public required string Title { get; set; }
    public string? Description { get; set; }
    public DateTime? PublishedAt { get; set; }
    public DateTime? ExpiresAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    public User User { get; set; } = null!;
    public ICollection<ListingImage> ListingImages { get; set; } = [];
    public ICollection<ChatRoom> ChatRooms { get; set; } = [];
    public ICollection<Favorite> Favorites { get; set; } = [];
}
