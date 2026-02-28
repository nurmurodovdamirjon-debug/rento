using Microsoft.EntityFrameworkCore;
using Rento.Application.Common;
using Rento.Application.DTOs.Listings;
using Rento.Application.Repositories;
using Rento.Core.Common;
using Rento.Core.Entities;
using Rento.Infrastructure.Data;

namespace Rento.Infrastructure.Repositories;

public class ListingRepository(RentoDbContext db) : IListingRepository
{

    public async Task<Listing?> GetByIdAsync(Guid id, CancellationToken ct = default)
    {
        return await db.Listings.AsNoTracking().FirstOrDefaultAsync(l => l.Id == id, ct);
    }

    public async Task<Listing?> GetByIdWithImagesAsync(Guid id, CancellationToken ct = default)
    {
        return await db.Listings
            .AsNoTracking()
            .Include(l => l.ListingImages.OrderBy(i => i.SortOrder))
            .Include(l => l.User)
            .FirstOrDefaultAsync(l => l.Id == id, ct);
    }

    public async Task<PaginatedResult<Listing>> GetListingsAsync(ListingFilter filter, CancellationToken ct = default)
    {
        var q = db.Listings.AsNoTracking().Where(l => l.Status == ListingStatus.Active);
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

        // Include before Skip/Take to avoid loading all rows into memory
        var items = await q
            .Include(l => l.ListingImages.Where(i => i.IsMain))
            .Skip((page - 1) * perPage)
            .Take(perPage)
            .ToListAsync(ct);

        return PaginatedResult<Listing>.Create(items, page, perPage, total);
    }

    public async Task<PaginatedResult<Listing>> GetMyListingsAsync(Guid userId, string? status, int page, int perPage, CancellationToken ct = default)
    {
        var q = db.Listings.AsNoTracking().Where(l => l.UserId == userId);
        if (!string.IsNullOrWhiteSpace(status)) q = q.Where(l => l.Status == status);
        var total = await q.CountAsync(ct);

        // Include before Skip/Take
        var items = await q
            .OrderByDescending(l => l.CreatedAt)
            .Include(l => l.ListingImages.Where(i => i.IsMain))
            .Skip((page - 1) * perPage)
            .Take(perPage)
            .ToListAsync(ct);

        return PaginatedResult<Listing>.Create(items, page, perPage, total);
    }

    public async Task<Listing> CreateAsync(Listing listing, CancellationToken ct = default)
    {
        listing.Id = Guid.NewGuid();
        listing.CreatedAt = DateTime.UtcNow;
        listing.UpdatedAt = DateTime.UtcNow;
        listing.Status = ListingStatus.Pending;
        db.Listings.Add(listing);
        await db.SaveChangesAsync(ct);
        return listing;
    }

    public async Task<bool> UpdateAsync(Listing listing, CancellationToken ct = default)
    {
        var updated = await db.Listings
            .Where(l => l.Id == listing.Id)
            .ExecuteUpdateAsync(s => s
                .SetProperty(l => l.Type, listing.Type)
                .SetProperty(l => l.DealType, listing.DealType)
                .SetProperty(l => l.City, listing.City)
                .SetProperty(l => l.District, listing.District)
                .SetProperty(l => l.Address, listing.Address)
                .SetProperty(l => l.Landmark, listing.Landmark)
                .SetProperty(l => l.Latitude, listing.Latitude)
                .SetProperty(l => l.Longitude, listing.Longitude)
                .SetProperty(l => l.Rooms, listing.Rooms)
                .SetProperty(l => l.Floor, listing.Floor)
                .SetProperty(l => l.TotalFloors, listing.TotalFloors)
                .SetProperty(l => l.AreaSqm, listing.AreaSqm)
                .SetProperty(l => l.Price, listing.Price)
                .SetProperty(l => l.Currency, listing.Currency)
                .SetProperty(l => l.PriceNegotiable, listing.PriceNegotiable)
                .SetProperty(l => l.HasFurniture, listing.HasFurniture)
                .SetProperty(l => l.HasAppliances, listing.HasAppliances)
                .SetProperty(l => l.HasInternet, listing.HasInternet)
                .SetProperty(l => l.HasParking, listing.HasParking)
                .SetProperty(l => l.HasConditioner, listing.HasConditioner)
                .SetProperty(l => l.AllowsPets, listing.AllowsPets)
                .SetProperty(l => l.AllowsChildren, listing.AllowsChildren)
                .SetProperty(l => l.UtilitiesIncluded, listing.UtilitiesIncluded)
                .SetProperty(l => l.DepositAmount, listing.DepositAmount)
                .SetProperty(l => l.Title, listing.Title)
                .SetProperty(l => l.Description, listing.Description)
                .SetProperty(l => l.UpdatedAt, DateTime.UtcNow), ct);
        return updated > 0;
    }

    public async Task<bool> DeleteAsync(Guid id, Guid userId, CancellationToken ct = default)
    {
        var deleted = await db.Listings
            .Where(l => l.Id == id && l.UserId == userId)
            .ExecuteDeleteAsync(ct);
        return deleted > 0;
    }

    public async Task<int> CountCreatedTodayAsync(Guid userId, CancellationToken ct = default)
    {
        var today = DateTime.UtcNow.Date;
        return await db.Listings.CountAsync(l => l.UserId == userId && l.CreatedAt >= today, ct);
    }

    public async Task<bool> UpdateStatusAsync(Guid listingId, Guid userId, string status, CancellationToken ct = default)
    {
        var updated = await db.Listings
            .Where(l => l.Id == listingId && l.UserId == userId)
            .ExecuteUpdateAsync(s => s
                .SetProperty(l => l.Status, status)
                .SetProperty(l => l.UpdatedAt, DateTime.UtcNow), ct);
        return updated > 0;
    }

    public async Task<PaginatedResult<Listing>> SearchListingsAsync(SearchFilter filter, CancellationToken ct = default)
    {
        var q = db.Listings.AsNoTracking().Where(l => l.Status == ListingStatus.Active);
        if (!string.IsNullOrWhiteSpace(filter.Q))
        {
            var term = $"%{filter.Q.Trim()}%";
            // ILike uses PostgreSQL case-insensitive index scan (much faster than ToLower().Contains())
            q = q.Where(l => EF.Functions.ILike(l.Title ?? "", term)
                || EF.Functions.ILike(l.City ?? "", term)
                || EF.Functions.ILike(l.District ?? "", term));
        }
        if (!string.IsNullOrWhiteSpace(filter.City)) q = q.Where(l => l.City == filter.City);
        if (!string.IsNullOrWhiteSpace(filter.Type)) q = q.Where(l => l.Type == filter.Type);
        if (!string.IsNullOrWhiteSpace(filter.DealType)) q = q.Where(l => l.DealType == filter.DealType);

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

        // Include before Skip/Take
        var items = await q
            .Include(l => l.ListingImages.Where(i => i.IsMain))
            .Skip((page - 1) * perPage)
            .Take(perPage)
            .ToListAsync(ct);

        return PaginatedResult<Listing>.Create(items, page, perPage, total);
    }

    public async Task<PaginatedResult<Listing>> GetPendingListingsAsync(int page, int perPage, CancellationToken ct = default)
    {
        var q = db.Listings.AsNoTracking().Where(l => l.Status == ListingStatus.Pending).OrderBy(l => l.CreatedAt);
        var total = await q.CountAsync(ct);

        // Include before Skip/Take
        var items = await q
            .Include(l => l.ListingImages.Where(i => i.IsMain))
            .Include(l => l.User)
            .Skip((page - 1) * perPage)
            .Take(perPage)
            .ToListAsync(ct);

        return PaginatedResult<Listing>.Create(items, page, perPage, total);
    }

    public async Task<bool> UpdateStatusByAdminAsync(Guid listingId, string status, string? rejectionReason, CancellationToken ct = default)
    {
        var updated = await db.Listings
            .Where(l => l.Id == listingId)
            .ExecuteUpdateAsync(s => s
                .SetProperty(l => l.Status, status)
                .SetProperty(l => l.UpdatedAt, DateTime.UtcNow)
                .SetProperty(l => l.RejectionReason, rejectionReason), ct);
        return updated > 0;
    }
}
