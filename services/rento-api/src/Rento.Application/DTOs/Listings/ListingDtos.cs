using Rento.Core.Common;

namespace Rento.Application.DTOs.Listings;

public class ListingFilter
{
    public string? City { get; init; }
    public string? District { get; init; }
    public string? Type { get; init; }
    public string? DealType { get; init; }
    public int? RoomsMin { get; init; }
    public int? RoomsMax { get; init; }
    public decimal? PriceMin { get; init; }
    public decimal? PriceMax { get; init; }
    public string? Currency { get; init; }
    public bool? HasFurniture { get; init; }
    public bool? HasParking { get; init; }
    public bool? AllowsPets { get; init; }
    public string? Sort { get; init; }
    public int Page { get; init; } = 1;
    public int PerPage { get; init; } = 20;
}

public class SearchFilter
{
    public string? Q { get; init; }
    public string? City { get; init; }
    public string? Type { get; init; }
    public string? DealType { get; init; }
    public string? Sort { get; init; }
    public int Page { get; init; } = 1;
    public int PerPage { get; init; } = 20;
}

public class NearbyFilter
{
    public double Lat { get; init; }
    public double Lng { get; init; }
    public double RadiusKm { get; init; }
    public string? Type { get; init; }
    public string? DealType { get; init; }
    public int Page { get; init; } = 1;
    public int PerPage { get; init; } = 20;
}

public class ListingSummaryDto
{
    public Guid Id { get; init; }
    public required string Title { get; init; }
    public string? ImageUrl { get; init; }
    public decimal Price { get; init; }
    public string Currency { get; init; } = Rento.Core.Common.Currency.Uzs;
    public required string City { get; init; }
    public string? District { get; init; }
    public required string Type { get; init; }
    public required string DealType { get; init; }
    public short? Rooms { get; init; }
    public required string Status { get; init; }
    public DateTime CreatedAt { get; init; }
    public double? DistanceMeters { get; init; }
}

public class ListingDetailDto
{
    public Guid Id { get; init; }
    public Guid UserId { get; init; }
    public required string Type { get; init; }
    public required string DealType { get; init; }
    public required string City { get; init; }
    public string? District { get; init; }
    public string? Address { get; init; }
    public string? Landmark { get; init; }
    public decimal? Latitude { get; init; }
    public decimal? Longitude { get; init; }
    public short? Rooms { get; init; }
    public short? Floor { get; init; }
    public short? TotalFloors { get; init; }
    public decimal? AreaSqm { get; init; }
    public decimal Price { get; init; }
    public string Currency { get; init; } = Rento.Core.Common.Currency.Uzs;
    public bool PriceNegotiable { get; init; }
    public bool HasFurniture { get; init; }
    public bool HasAppliances { get; init; }
    public bool HasInternet { get; init; }
    public bool HasParking { get; init; }
    public bool HasConditioner { get; init; }
    public bool AllowsPets { get; init; }
    public bool AllowsChildren { get; init; }
    public bool UtilitiesIncluded { get; init; }
    public decimal? DepositAmount { get; init; }
    public required string Status { get; init; }
    public int ViewsCount { get; init; }
    public int FavoritesCount { get; init; }
    public int ContactsCount { get; init; }
    public required string Title { get; init; }
    public string? Description { get; init; }
    public DateTime? PublishedAt { get; init; }
    public DateTime CreatedAt { get; init; }
    public DateTime UpdatedAt { get; init; }
    public List<ListingImageDto> Images { get; init; } = [];
    public UserSummaryDto? User { get; init; }
}

public class ListingImageDto
{
    public Guid Id { get; init; }
    public required string Url { get; init; }
    public string? ThumbnailUrl { get; init; }
    public short SortOrder { get; init; }
    public bool IsMain { get; init; }
}

public class UserSummaryDto
{
    public Guid Id { get; init; }
    public string? FullName { get; init; }
    public string? AvatarUrl { get; init; }
    public required string Role { get; init; }
}

public class CreateListingRequest
{
    public required string Type { get; init; }
    public string? DealType { get; init; }
    public required string City { get; init; }
    public string? District { get; init; }
    public string? Address { get; init; }
    public string? Landmark { get; init; }
    public decimal? Latitude { get; init; }
    public decimal? Longitude { get; init; }
    public short? Rooms { get; init; }
    public short? Floor { get; init; }
    public short? TotalFloors { get; init; }
    public decimal? AreaSqm { get; init; }
    public decimal Price { get; init; }
    public string Currency { get; init; } = Rento.Core.Common.Currency.Uzs;
    public bool PriceNegotiable { get; init; }
    public bool HasFurniture { get; init; }
    public bool HasAppliances { get; init; }
    public bool HasInternet { get; init; }
    public bool HasParking { get; init; }
    public bool HasConditioner { get; init; }
    public bool AllowsPets { get; init; }
    public bool AllowsChildren { get; init; }
    public bool UtilitiesIncluded { get; init; }
    public decimal? DepositAmount { get; init; }
    public required string Title { get; init; }
    public string? Description { get; init; }
}

public class UpdateListingRequest : CreateListingRequest { }

public class ListingStatsDto
{
    public int Views { get; init; }
    public int Favorites { get; init; }
    public int Contacts { get; init; }
}
