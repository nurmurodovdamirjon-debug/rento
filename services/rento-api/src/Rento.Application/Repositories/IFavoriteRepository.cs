namespace Rento.Application.Repositories;

public interface IFavoriteRepository
{
    Task<bool> ExistsAsync(Guid userId, Guid listingId, CancellationToken ct = default);
    Task<bool> ToggleAsync(Guid userId, Guid listingId, CancellationToken ct = default);
    Task<(List<Core.Entities.Favorite> Items, int Total)> GetPageAsync(Guid userId, int page, int perPage, CancellationToken ct = default);
    Task<List<Guid>> GetListingIdsAsync(Guid userId, CancellationToken ct = default);
}
