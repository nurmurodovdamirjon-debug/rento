using Microsoft.EntityFrameworkCore;
using Rento.Application.Common;
using Rento.Application.DTOs.Listings;
using Rento.Application.Repositories;
using Rento.Core.Entities;
using Rento.Infrastructure.Data;

namespace Rento.Infrastructure.Repositories;

public class ListingRepository : IListingRepository
{
    private readonly RentoDbContext _db;

    public ListingRepository(RentoDbContext db)
    {
        _db = db;
    }

    public async Task<Listing?> GetByIdAsync(Guid id, CancellationToken ct = default)
    {
        return await _db.Listings.AsNoTracking().FirstOrDefaultAsync(l => l.Id == id, ct);
    }

    public async Task<Listing?> GetByIdWithImagesAsync(Guid id, CancellationToken ct = default)
    {
        return await _db.Listings
            .AsNoTracking()
            .Include(l => l.ListingImages.OrderBy(i => i.SortOrder))
            .Include(l => l.User)
            .FirstOrDefaultAsync(l => l.Id == id, ct);
    }

    public async Task<PaginatedResult<Listing>> GetListingsAsync(ListingFilter filter, CancellationToken ct = default)
    {
        var q = _db.Listings.AsNoTracking().Where(l => l.Status == "active");
        if (!string.IsNullOrWhiteSpace(filter.City)) q = q.Where(l => l.City == filter.City);
        if (!string.IsNullOrWhiteSpace(filter.District)) q = q.Where(l => l.District == filter.District);
        if (!string.IsNullOrWhiteSpace(filter.Type)) q = q.Where(l => l.Type == filter.Type);
        if (!string.IsNullOrWhiteSpace(filter.DealType)) q = q.Where(l => l.DealType == filter.DealType);
        if (filter.RoomsMin.HasValue) q = q.Where(l => l.Rooms >= filter.RoomsMin);
        if (filter.RoomsMax.HasValue) q = q.Where(l => l.Rooms <= filter.RoomsMax);
        if (filter.PriceMin.HasValue) q = q.Where(l => l.Price >= filter.PriceMin);
        if (filter.PriceMax.HasValue) q = q.Where(l => l.Price <= filter.PriceMax);
        if (!string.IsNullOrWhiteSpace(filter.Currency)) q = q.Where(l => l.Currency == filter.Currency);
        if (filter.HasFurniture.HasValue) q = q.Where(l => l.HasFurniture == filter.HasFurniture);
        if (filter.HasParking.HasValue) q = q.Where(l => l.HasParking == filter.HasParking);
        if (filter.AllowsPets.HasValue) q = q.Where(l => l.AllowsPets == filter.AllowsPets);

        var total = await q.CountAsync(ct);
        var sort = (filter.Sort ?? "newest").ToLowerInvariant();
        q = sort switch
        {
            "price_asc" => q.OrderBy(l => l.Price),
            "price_desc" => q.OrderByDescending(l => l.Price),
            "oldest" => q.OrderBy(l => l.CreatedAt),
            _ => q.OrderByDescending(l => l.CreatedAt)
        };

        var page = Math.Max(1, filter.Page);
        var perPage = Math.Clamp(filter.PerPage, 1, 100);
        var items = await q.Skip((page - 1) * perPage).Take(perPage)
            .Include(l => l.ListingImages.Where(i => i.IsMain))
            .ToListAsync(ct);

        var totalPages = (int)Math.Ceiling(total / (double)perPage);
        return new PaginatedResult<Listing>
        {
            Items = items,
            Page = page,
            PerPage = perPage,
            Total = total,
            TotalPages = totalPages
        };
    }

    public async Task<PaginatedResult<Listing>> GetMyListingsAsync(Guid userId, string? status, int page, int perPage, CancellationToken ct = default)
    {
        var q = _db.Listings.AsNoTracking().Where(l => l.UserId == userId);
        if (!string.IsNullOrWhiteSpace(status)) q = q.Where(l => l.Status == status);
        var total = await q.CountAsync(ct);
        var items = await q.OrderByDescending(l => l.CreatedAt)
            .Skip((page - 1) * perPage).Take(perPage)
            .Include(l => l.ListingImages.Where(i => i.IsMain))
            .ToListAsync(ct);
        var totalPages = (int)Math.Ceiling(total / (double)perPage);
        return new PaginatedResult<Listing>
        {
            Items = items,
            Page = page,
            PerPage = perPage,
            Total = total,
            TotalPages = totalPages
        };
    }

    public async Task<Listing> CreateAsync(Listing listing, CancellationToken ct = default)
    {
        listing.Id = Guid.NewGuid();
        listing.CreatedAt = DateTime.UtcNow;
        listing.UpdatedAt = DateTime.UtcNow;
        listing.Status = "pending";
        _db.Listings.Add(listing);
        await _db.SaveChangesAsync(ct);
        return listing;
    }

    public async Task<bool> UpdateAsync(Listing listing, CancellationToken ct = default)
    {
        var existing = await _db.Listings.FirstOrDefaultAsync(l => l.Id == listing.Id, ct);
        if (existing == null) return false;
        existing.Type = listing.Type;
        existing.DealType = listing.DealType;
        existing.City = listing.City;
        existing.District = listing.District;
        existing.Address = listing.Address;
        existing.Landmark = listing.Landmark;
        existing.Latitude = listing.Latitude;
        existing.Longitude = listing.Longitude;
        existing.Rooms = listing.Rooms;
        existing.Floor = listing.Floor;
        existing.TotalFloors = listing.TotalFloors;
        existing.AreaSqm = listing.AreaSqm;
        existing.Price = listing.Price;
        existing.Currency = listing.Currency;
        existing.PriceNegotiable = listing.PriceNegotiable;
        existing.HasFurniture = listing.HasFurniture;
        existing.HasAppliances = listing.HasAppliances;
        existing.HasInternet = listing.HasInternet;
        existing.HasParking = listing.HasParking;
        existing.HasConditioner = listing.HasConditioner;
        existing.AllowsPets = listing.AllowsPets;
        existing.AllowsChildren = listing.AllowsChildren;
        existing.UtilitiesIncluded = listing.UtilitiesIncluded;
        existing.DepositAmount = listing.DepositAmount;
        existing.Title = listing.Title;
        existing.Description = listing.Description;
        existing.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync(ct);
        return true;
    }

    public async Task<bool> DeleteAsync(Guid id, Guid userId, CancellationToken ct = default)
    {
        var listing = await _db.Listings.FirstOrDefaultAsync(l => l.Id == id && l.UserId == userId, ct);
        if (listing == null) return false;
        _db.Listings.Remove(listing);
        await _db.SaveChangesAsync(ct);
        return true;
    }

    public async Task<int> CountCreatedTodayAsync(Guid userId, CancellationToken ct = default)
    {
        var today = DateTime.UtcNow.Date;
        return await _db.Listings.CountAsync(l => l.UserId == userId && l.CreatedAt >= today, ct);
    }

    public async Task<bool> UpdateStatusAsync(Guid listingId, Guid userId, string status, CancellationToken ct = default)
    {
        var updated = await _db.Listings
            .Where(l => l.Id == listingId && l.UserId == userId)
            .ExecuteUpdateAsync(s => s.SetProperty(l => l.Status, status).SetProperty(l => l.UpdatedAt, DateTime.UtcNow), ct);
        return updated > 0;
    }
}
