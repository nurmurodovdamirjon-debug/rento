using Rento.Application.Common;

namespace Rento.Application.Services;

public interface IFavoriteService
{
    Task<ToggleFavoriteResult> ToggleAsync(Guid userId, Guid listingId, CancellationToken ct = default);
    Task<PaginatedResult<FavoriteItemDto>> GetListAsync(Guid userId, int page, int perPage, CancellationToken ct = default);
    Task<bool> CheckAsync(Guid userId, Guid listingId, CancellationToken ct = default);
    Task<List<Guid>> GetIdsAsync(Guid userId, CancellationToken ct = default);
}

public class ToggleFavoriteResult
{
    public bool IsFavorite { get; set; }
}

public class FavoriteItemDto
{
    public Guid ListingId { get; set; }
    public object? Listing { get; set; }
    public DateTime CreatedAt { get; set; }
}
