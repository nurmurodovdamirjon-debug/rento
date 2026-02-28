using Rento.Application.Common;
using Rento.Application.DTOs.Listings;
using Rento.Application.Repositories;
using Rento.Core.Entities;

namespace Rento.Application.Services.Implementations;

public class ListingService : IListingService
{
    private const int DailyCreateLimit = 10;
    private readonly IListingRepository _listingRepo;
    private readonly Microsoft.Extensions.Configuration.IConfiguration _config;

    public ListingService(IListingRepository listingRepo, Microsoft.Extensions.Configuration.IConfiguration config)
    {
        _listingRepo = listingRepo;
        _config = config;
    }

    public async Task<PaginatedResult<ListingSummaryDto>> GetListingsAsync(ListingFilter filter, CancellationToken ct = default)
    {
        var result = await _listingRepo.GetListingsAsync(filter, ct);
        return new PaginatedResult<ListingSummaryDto>
        {
            Items = result.Items.Select(MapToSummary).ToList(),
            Page = result.Page,
            PerPage = result.PerPage,
            Total = result.Total,
            TotalPages = result.TotalPages
        };
    }

    public Task<PaginatedResult<ListingSummaryDto>> SearchAsync(SearchFilter filter, CancellationToken ct = default)
    {
        // TODO: Elasticsearch
        return Task.FromResult(new PaginatedResult<ListingSummaryDto>
        {
            Items = new List<ListingSummaryDto>(),
            Page = filter.Page,
            PerPage = filter.PerPage,
            Total = 0,
            TotalPages = 0
        });
    }

    public Task<PaginatedResult<ListingSummaryDto>> GetNearbyAsync(NearbyFilter filter, CancellationToken ct = default)
    {
        // TODO: PostGIS
        return Task.FromResult(new PaginatedResult<ListingSummaryDto>
        {
            Items = new List<ListingSummaryDto>(),
            Page = filter.Page,
            PerPage = filter.PerPage,
            Total = 0,
            TotalPages = 0
        });
    }

    public async Task<ListingDetailDto?> GetByIdAsync(Guid id, Guid? userId, CancellationToken ct = default)
    {
        var listing = await _listingRepo.GetByIdWithImagesAsync(id, ct);
        return listing == null ? null : MapToDetail(listing);
    }

    public async Task<PaginatedResult<ListingSummaryDto>> GetMyListingsAsync(Guid userId, string? status, int page, int perPage, CancellationToken ct = default)
    {
        var result = await _listingRepo.GetMyListingsAsync(userId, status, page, perPage, ct);
        return new PaginatedResult<ListingSummaryDto>
        {
            Items = result.Items.Select(MapToSummary).ToList(),
            Page = result.Page,
            PerPage = result.PerPage,
            Total = result.Total,
            TotalPages = result.TotalPages
        };
    }

    public async Task<ListingDetailDto?> CreateAsync(Guid userId, CreateListingRequest request, CancellationToken ct = default)
    {
        var todayCount = await _listingRepo.CountCreatedTodayAsync(userId, ct);
        if (todayCount >= DailyCreateLimit)
            return null;

        var listing = new Listing
        {
            UserId = userId,
            Type = request.Type,
            DealType = request.DealType ?? "rent",
            City = request.City,
            District = request.District,
            Address = request.Address,
            Landmark = request.Landmark,
            Latitude = request.Latitude,
            Longitude = request.Longitude,
            Rooms = request.Rooms,
            Floor = request.Floor,
            TotalFloors = request.TotalFloors,
            AreaSqm = request.AreaSqm,
            Price = request.Price,
            Currency = request.Currency ?? "UZS",
            PriceNegotiable = request.PriceNegotiable,
            HasFurniture = request.HasFurniture,
            HasAppliances = request.HasAppliances,
            HasInternet = request.HasInternet,
            HasParking = request.HasParking,
            HasConditioner = request.HasConditioner,
            AllowsPets = request.AllowsPets,
            AllowsChildren = request.AllowsChildren,
            UtilitiesIncluded = request.UtilitiesIncluded,
            DepositAmount = request.DepositAmount,
            Title = request.Title,
            Description = request.Description,
            Status = "pending"
        };
        var created = await _listingRepo.CreateAsync(listing, ct);
        return await GetByIdAsync(created.Id, userId, ct);
    }

    public async Task<ListingDetailDto?> UpdateAsync(Guid listingId, Guid userId, UpdateListingRequest request, CancellationToken ct = default)
    {
        var existing = await _listingRepo.GetByIdAsync(listingId, ct);
        if (existing == null || existing.UserId != userId) return null;

        existing.Type = request.Type;
        existing.DealType = request.DealType ?? "rent";
        existing.City = request.City;
        existing.District = request.District;
        existing.Address = request.Address;
        existing.Landmark = request.Landmark;
        existing.Latitude = request.Latitude;
        existing.Longitude = request.Longitude;
        existing.Rooms = request.Rooms;
        existing.Floor = request.Floor;
        existing.TotalFloors = request.TotalFloors;
        existing.AreaSqm = request.AreaSqm;
        existing.Price = request.Price;
        existing.Currency = request.Currency ?? "UZS";
        existing.PriceNegotiable = request.PriceNegotiable;
        existing.HasFurniture = request.HasFurniture;
        existing.HasAppliances = request.HasAppliances;
        existing.HasInternet = request.HasInternet;
        existing.HasParking = request.HasParking;
        existing.HasConditioner = request.HasConditioner;
        existing.AllowsPets = request.AllowsPets;
        existing.AllowsChildren = request.AllowsChildren;
        existing.UtilitiesIncluded = request.UtilitiesIncluded;
        existing.DepositAmount = request.DepositAmount;
        existing.Title = request.Title;
        existing.Description = request.Description;

        await _listingRepo.UpdateAsync(existing, ct);
        return await GetByIdAsync(listingId, userId, ct);
    }

    public Task<bool> DeleteAsync(Guid listingId, Guid userId, CancellationToken ct = default)
    {
        return _listingRepo.DeleteAsync(listingId, userId, ct);
    }

    public async Task<bool> UpdateStatusAsync(Guid listingId, Guid userId, string status, CancellationToken ct = default)
    {
        return await _listingRepo.UpdateStatusAsync(listingId, userId, status, ct);
    }

    public async Task<ListingStatsDto?> GetStatsAsync(Guid listingId, Guid userId, CancellationToken ct = default)
    {
        var listing = await _listingRepo.GetByIdAsync(listingId, ct);
        if (listing == null || listing.UserId != userId) return null;
        return new ListingStatsDto
        {
            Views = listing.ViewsCount,
            Favorites = listing.FavoritesCount,
            Contacts = listing.ContactsCount
        };
    }

    private static ListingSummaryDto MapToSummary(Listing l)
    {
        var mainImage = l.ListingImages?.FirstOrDefault(i => i.IsMain) ?? l.ListingImages?.FirstOrDefault();
        return new ListingSummaryDto
        {
            Id = l.Id,
            Title = l.Title,
            ImageUrl = mainImage?.Url,
            Price = l.Price,
            Currency = l.Currency,
            City = l.City,
            District = l.District,
            Type = l.Type,
            DealType = l.DealType,
            Rooms = l.Rooms,
            Status = l.Status,
            CreatedAt = l.CreatedAt
        };
    }

    private static ListingDetailDto MapToDetail(Listing l)
    {
        var dto = new ListingDetailDto
        {
            Id = l.Id,
            UserId = l.UserId,
            Type = l.Type,
            DealType = l.DealType,
            City = l.City,
            District = l.District,
            Address = l.Address,
            Landmark = l.Landmark,
            Latitude = l.Latitude,
            Longitude = l.Longitude,
            Rooms = l.Rooms,
            Floor = l.Floor,
            TotalFloors = l.TotalFloors,
            AreaSqm = l.AreaSqm,
            Price = l.Price,
            Currency = l.Currency,
            PriceNegotiable = l.PriceNegotiable,
            HasFurniture = l.HasFurniture,
            HasAppliances = l.HasAppliances,
            HasInternet = l.HasInternet,
            HasParking = l.HasParking,
            HasConditioner = l.HasConditioner,
            AllowsPets = l.AllowsPets,
            AllowsChildren = l.AllowsChildren,
            UtilitiesIncluded = l.UtilitiesIncluded,
            DepositAmount = l.DepositAmount,
            Status = l.Status,
            ViewsCount = l.ViewsCount,
            FavoritesCount = l.FavoritesCount,
            ContactsCount = l.ContactsCount,
            Title = l.Title,
            Description = l.Description,
            PublishedAt = l.PublishedAt,
            CreatedAt = l.CreatedAt,
            UpdatedAt = l.UpdatedAt,
            Images = (l.ListingImages ?? new List<ListingImage>()).OrderBy(i => i.SortOrder).Select(i => new ListingImageDto
            {
                Id = i.Id,
                Url = i.Url,
                ThumbnailUrl = i.ThumbnailUrl,
                SortOrder = i.SortOrder,
                IsMain = i.IsMain
            }).ToList()
        };
        if (l.User != null)
            dto.User = new UserSummaryDto
            {
                Id = l.User.Id,
                FullName = l.User.FullName,
                AvatarUrl = l.User.AvatarUrl,
                Role = l.User.Role
            };
        return dto;
    }
}
