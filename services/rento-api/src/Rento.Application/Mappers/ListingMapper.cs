using Rento.Application.DTOs.Listings;
using Rento.Core.Common;
using Rento.Core.Entities;

namespace Rento.Application.Mappers;

public static class ListingMapper
{
    public static ListingSummaryDto ToSummary(Listing listing)
    {
        var mainImage = listing.ListingImages?.FirstOrDefault(i => i.IsMain) ?? listing.ListingImages?.FirstOrDefault();

        return new ListingSummaryDto
        {
            Id = listing.Id,
            Title = listing.Title,
            ImageUrl = mainImage?.Url,
            Price = listing.Price,
            Currency = listing.Currency,
            City = listing.City,
            District = listing.District,
            Type = listing.Type,
            DealType = listing.DealType,
            Rooms = listing.Rooms,
            Status = listing.Status,
            CreatedAt = listing.CreatedAt
        };
    }

    public static ListingDetailDto ToDetail(Listing listing)
    {
        var dto = new ListingDetailDto
        {
            Id = listing.Id,
            UserId = listing.UserId,
            Type = listing.Type,
            DealType = listing.DealType,
            City = listing.City,
            District = listing.District,
            Address = listing.Address,
            Landmark = listing.Landmark,
            Latitude = listing.Latitude,
            Longitude = listing.Longitude,
            Rooms = listing.Rooms,
            Floor = listing.Floor,
            TotalFloors = listing.TotalFloors,
            AreaSqm = listing.AreaSqm,
            Price = listing.Price,
            Currency = listing.Currency,
            PriceNegotiable = listing.PriceNegotiable,
            HasFurniture = listing.HasFurniture,
            HasAppliances = listing.HasAppliances,
            HasInternet = listing.HasInternet,
            HasParking = listing.HasParking,
            HasConditioner = listing.HasConditioner,
            AllowsPets = listing.AllowsPets,
            AllowsChildren = listing.AllowsChildren,
            UtilitiesIncluded = listing.UtilitiesIncluded,
            DepositAmount = listing.DepositAmount,
            Status = listing.Status,
            ViewsCount = listing.ViewsCount,
            FavoritesCount = listing.FavoritesCount,
            ContactsCount = listing.ContactsCount,
            Title = listing.Title,
            Description = listing.Description,
            PublishedAt = listing.PublishedAt,
            CreatedAt = listing.CreatedAt,
            UpdatedAt = listing.UpdatedAt,
            User = listing.User != null
                ? new UserSummaryDto
                {
                    Id = listing.User.Id,
                    FullName = listing.User.FullName,
                    AvatarUrl = listing.User.AvatarUrl,
                    Role = listing.User.Role
                }
                : null,
            Images = (listing.ListingImages ?? [])
                .OrderBy(i => i.SortOrder)
                .Select(i => new ListingImageDto
                {
                    Id = i.Id,
                    Url = i.Url,
                    ThumbnailUrl = i.ThumbnailUrl,
                    SortOrder = i.SortOrder,
                    IsMain = i.IsMain
                })
                .ToList()
        };

        return dto;
    }

    public static Listing ToEntity(Guid userId, CreateListingRequest request) =>
        new()
        {
            UserId = userId,
            Type = request.Type,
            DealType = request.DealType ?? ListingDealType.Rent,
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
            Currency = request.Currency ?? Currency.Uzs,
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
            Status = ListingStatus.Pending
        };

    public static void ApplyUpdate(Listing listing, UpdateListingRequest request)
    {
        listing.Type = request.Type;
        listing.DealType = request.DealType ?? ListingDealType.Rent;
        listing.City = request.City;
        listing.District = request.District;
        listing.Address = request.Address;
        listing.Landmark = request.Landmark;
        listing.Latitude = request.Latitude;
        listing.Longitude = request.Longitude;
        listing.Rooms = request.Rooms;
        listing.Floor = request.Floor;
        listing.TotalFloors = request.TotalFloors;
        listing.AreaSqm = request.AreaSqm;
        listing.Price = request.Price;
        listing.Currency = request.Currency ?? Currency.Uzs;
        listing.PriceNegotiable = request.PriceNegotiable;
        listing.HasFurniture = request.HasFurniture;
        listing.HasAppliances = request.HasAppliances;
        listing.HasInternet = request.HasInternet;
        listing.HasParking = request.HasParking;
        listing.HasConditioner = request.HasConditioner;
        listing.AllowsPets = request.AllowsPets;
        listing.AllowsChildren = request.AllowsChildren;
        listing.UtilitiesIncluded = request.UtilitiesIncluded;
        listing.DepositAmount = request.DepositAmount;
        listing.Title = request.Title;
        listing.Description = request.Description;
    }
}
