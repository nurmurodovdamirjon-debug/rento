using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Core.Entities;

namespace Rento.Application.Services.Implementations;

public class FavoriteService(IFavoriteRepository repo) : IFavoriteService
{
    public async Task<ToggleFavoriteResult> ToggleAsync(Guid userId, Guid listingId, CancellationToken ct = default)
    {
        var isNowFavorite = await repo.ToggleAsync(userId, listingId, ct);
        return new ToggleFavoriteResult { IsFavorite = isNowFavorite };
    }

    public async Task<PaginatedResult<FavoriteItemDto>> GetListAsync(Guid userId, int page, int perPage, CancellationToken ct = default)
    {
        var (items, total) = await repo.GetPageAsync(userId, page, perPage, ct);
        var dtos = items.Select(f => new FavoriteItemDto
        {
            ListingId = f.ListingId,
            Listing = f.Listing != null ? new { f.Listing.Id, f.Listing.Title, f.Listing.Price, f.Listing.Currency, ImageUrl = f.Listing.ListingImages?.FirstOrDefault(i => i.IsMain)?.Url ?? f.Listing.ListingImages?.FirstOrDefault()?.Url } : null,
            CreatedAt = f.CreatedAt
        }).ToList();
        return PaginatedResult<FavoriteItemDto>.Create(dtos, page, perPage, total);
    }

    public Task<bool> CheckAsync(Guid userId, Guid listingId, CancellationToken ct = default)
        => repo.ExistsAsync(userId, listingId, ct);

    public Task<List<Guid>> GetIdsAsync(Guid userId, CancellationToken ct = default)
        => repo.GetListingIdsAsync(userId, ct);
}
