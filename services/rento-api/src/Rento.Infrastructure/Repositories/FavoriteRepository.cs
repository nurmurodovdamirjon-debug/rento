using Microsoft.EntityFrameworkCore;
using Rento.Application.Repositories;
using Rento.Core.Entities;
using Rento.Infrastructure.Data;

namespace Rento.Infrastructure.Repositories;

public class FavoriteRepository : IFavoriteRepository
{
    private readonly RentoDbContext _db;

    public FavoriteRepository(RentoDbContext db) => _db = db;

    public async Task<bool> ExistsAsync(Guid userId, Guid listingId, CancellationToken ct = default)
    {
        return await _db.Favorites.AnyAsync(f => f.UserId == userId && f.ListingId == listingId, ct);
    }

    public async Task<bool> ToggleAsync(Guid userId, Guid listingId, CancellationToken ct = default)
    {
        var existing = await _db.Favorites.FirstOrDefaultAsync(f => f.UserId == userId && f.ListingId == listingId, ct);
        if (existing != null)
        {
            _db.Favorites.Remove(existing);
            await _db.SaveChangesAsync(ct);
            return false;
        }
        _db.Favorites.Add(new Favorite { UserId = userId, ListingId = listingId });
        await _db.SaveChangesAsync(ct);
        return true;
    }

    public async Task<(List<Favorite> Items, int Total)> GetPageAsync(Guid userId, int page, int perPage, CancellationToken ct = default)
    {
        var q = _db.Favorites.AsNoTracking().Where(f => f.UserId == userId).OrderByDescending(f => f.CreatedAt);
        var total = await q.CountAsync(ct);
        var items = await q.Include(f => f.Listing).ThenInclude(l => l!.ListingImages.Where(i => i.IsMain))
            .Skip((page - 1) * perPage).Take(perPage).ToListAsync(ct);
        return (items, total);
    }

    public async Task<List<Guid>> GetListingIdsAsync(Guid userId, CancellationToken ct = default)
    {
        return await _db.Favorites.AsNoTracking().Where(f => f.UserId == userId).Select(f => f.ListingId).ToListAsync(ct);
    }
}
