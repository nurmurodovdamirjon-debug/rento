namespace Rento.Application.DTOs.Listings;

public class ListingFilter
{
    public string? City { get; set; }
    public string? District { get; set; }
    public string? Type { get; set; }
    public string? DealType { get; set; }
    public int? RoomsMin { get; set; }
    public int? RoomsMax { get; set; }
    public decimal? PriceMin { get; set; }
    public decimal? PriceMax { get; set; }
    public string? Currency { get; set; }
    public bool? HasFurniture { get; set; }
    public bool? HasParking { get; set; }
    public bool? AllowsPets { get; set; }
    public string? Sort { get; set; }
    public int Page { get; set; } = 1;
    public int PerPage { get; set; } = 20;
}

public class SearchFilter
{
    public string? Q { get; set; }
    public string? City { get; set; }
    public string? Type { get; set; }
    public string? DealType { get; set; }
    public string? Sort { get; set; }
    public int Page { get; set; } = 1;
    public int PerPage { get; set; } = 20;
}

public class NearbyFilter
{
    public double Lat { get; set; }
    public double Lng { get; set; }
    public double RadiusKm { get; set; }
    public string? Type { get; set; }
    public string? DealType { get; set; }
    public int Page { get; set; } = 1;
    public int PerPage { get; set; } = 20;
}

public class ListingSummaryDto
{
    public Guid Id { get; set; }
    public string Title { get; set; } = "";
    public string? ImageUrl { get; set; }
    public decimal Price { get; set; }
    public string Currency { get; set; } = "UZS";
    public string City { get; set; } = "";
    public string? District { get; set; }
    public string Type { get; set; } = "";
    public string DealType { get; set; } = "";
    public short? Rooms { get; set; }
    public string Status { get; set; } = "";
    public DateTime CreatedAt { get; set; }
    public double? DistanceMeters { get; set; }
}

public class ListingDetailDto
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public string Type { get; set; } = "";
    public string DealType { get; set; } = "";
    public string City { get; set; } = "";
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
    public string Currency { get; set; } = "UZS";
    public bool PriceNegotiable { get; set; }
    public bool HasFurniture { get; set; }
    public bool HasAppliances { get; set; }
    public bool HasInternet { get; set; }
    public bool HasParking { get; set; }
    public bool HasConditioner { get; set; }
    public bool AllowsPets { get; set; }
    public bool AllowsChildren { get; set; }
    public bool UtilitiesIncluded { get; set; }
    public decimal? DepositAmount { get; set; }
    public string Status { get; set; } = "";
    public int ViewsCount { get; set; }
    public int FavoritesCount { get; set; }
    public int ContactsCount { get; set; }
    public string Title { get; set; } = "";
    public string? Description { get; set; }
    public DateTime? PublishedAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public List<ListingImageDto> Images { get; set; } = new();
    public UserSummaryDto? User { get; set; }
}

public class ListingImageDto
{
    public Guid Id { get; set; }
    public string Url { get; set; } = "";
    public string? ThumbnailUrl { get; set; }
    public short SortOrder { get; set; }
    public bool IsMain { get; set; }
}

public class UserSummaryDto
{
    public Guid Id { get; set; }
    public string? FullName { get; set; }
    public string? AvatarUrl { get; set; }
    public string Role { get; set; } = "";
}

public class CreateListingRequest
{
    public string Type { get; set; } = "";
    public string? DealType { get; set; }
    public string City { get; set; } = "";
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
    public string Currency { get; set; } = "UZS";
    public bool PriceNegotiable { get; set; }
    public bool HasFurniture { get; set; }
    public bool HasAppliances { get; set; }
    public bool HasInternet { get; set; }
    public bool HasParking { get; set; }
    public bool HasConditioner { get; set; }
    public bool AllowsPets { get; set; }
    public bool AllowsChildren { get; set; }
    public bool UtilitiesIncluded { get; set; }
    public decimal? DepositAmount { get; set; }
    public string Title { get; set; } = "";
    public string? Description { get; set; }
}

public class UpdateListingRequest : CreateListingRequest { }

public class ListingStatsDto
{
    public int Views { get; set; }
    public int Favorites { get; set; }
    public int Contacts { get; set; }
}
