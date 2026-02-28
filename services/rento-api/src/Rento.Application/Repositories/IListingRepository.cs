using Rento.Application.Common;
using Rento.Application.DTOs.Listings;
using Rento.Core.Entities;

namespace Rento.Application.Repositories;

public interface IListingRepository
{
    Task<Listing?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<Listing?> GetByIdWithImagesAsync(Guid id, CancellationToken ct = default);
    Task<PaginatedResult<Listing>> GetListingsAsync(ListingFilter filter, CancellationToken ct = default);
    Task<PaginatedResult<Listing>> GetMyListingsAsync(Guid userId, string? status, int page, int perPage, CancellationToken ct = default);
    Task<Listing> CreateAsync(Listing listing, CancellationToken ct = default);
    Task<bool> UpdateAsync(Listing listing, CancellationToken ct = default);
    Task<bool> DeleteAsync(Guid id, Guid userId, CancellationToken ct = default);
    Task<int> CountCreatedTodayAsync(Guid userId, CancellationToken ct = default);
    Task<bool> UpdateStatusAsync(Guid listingId, Guid userId, string status, CancellationToken ct = default);
}
