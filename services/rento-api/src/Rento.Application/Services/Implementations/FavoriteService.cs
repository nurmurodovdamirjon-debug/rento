using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Core.Entities;

namespace Rento.Application.Services.Implementations;

public class FavoriteService : IFavoriteService
{
    private readonly IFavoriteRepository _repo;

    public FavoriteService(IFavoriteRepository repo)
    {
        _repo = repo;
    }

    public async Task<ToggleFavoriteResult> ToggleAsync(Guid userId, Guid listingId, CancellationToken ct = default)
    {
        var isNowFavorite = await _repo.ToggleAsync(userId, listingId, ct);
        return new ToggleFavoriteResult { IsFavorite = isNowFavorite };
    }

    public async Task<PaginatedResult<FavoriteItemDto>> GetListAsync(Guid userId, int page, int perPage, CancellationToken ct = default)
    {
        var (items, total) = await _repo.GetPageAsync(userId, page, perPage, ct);
        var dtos = items.Select(f => new FavoriteItemDto
        {
            ListingId = f.ListingId,
            Listing = f.Listing != null ? new { f.Listing.Id, f.Listing.Title, f.Listing.Price, f.Listing.Currency, ImageUrl = f.Listing.ListingImages?.FirstOrDefault(i => i.IsMain)?.Url ?? f.Listing.ListingImages?.FirstOrDefault()?.Url } : null,
            CreatedAt = f.CreatedAt
        }).ToList();
        var totalPages = (int)Math.Ceiling(total / (double)perPage);
        return new PaginatedResult<FavoriteItemDto>
        {
            Items = dtos,
            Page = page,
            PerPage = perPage,
            Total = total,
            TotalPages = totalPages
        };
    }

    public Task<bool> CheckAsync(Guid userId, Guid listingId, CancellationToken ct = default)
        => _repo.ExistsAsync(userId, listingId, ct);

    public Task<List<Guid>> GetIdsAsync(Guid userId, CancellationToken ct = default)
        => _repo.GetListingIdsAsync(userId, ct);
}
