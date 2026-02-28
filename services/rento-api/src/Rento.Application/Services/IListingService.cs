using Rento.Application.Common;
using Rento.Application.DTOs.Listings;

namespace Rento.Application.Services;

public interface IListingService
{
    Task<PaginatedResult<ListingSummaryDto>> GetListingsAsync(ListingFilter filter, CancellationToken ct = default);
    Task<PaginatedResult<ListingSummaryDto>> SearchAsync(SearchFilter filter, CancellationToken ct = default);
    Task<PaginatedResult<ListingSummaryDto>> GetNearbyAsync(NearbyFilter filter, CancellationToken ct = default);
    Task<ListingDetailDto?> GetByIdAsync(Guid id, Guid? userId, CancellationToken ct = default);
    Task<PaginatedResult<ListingSummaryDto>> GetMyListingsAsync(Guid userId, string? status, int page, int perPage, CancellationToken ct = default);
    Task<ListingDetailDto?> CreateAsync(Guid userId, CreateListingRequest request, CancellationToken ct = default);
    Task<ListingDetailDto?> UpdateAsync(Guid listingId, Guid userId, UpdateListingRequest request, CancellationToken ct = default);
    Task<bool> DeleteAsync(Guid listingId, Guid userId, CancellationToken ct = default);
    Task<bool> UpdateStatusAsync(Guid listingId, Guid userId, string status, CancellationToken ct = default);
    Task<ListingStatsDto?> GetStatsAsync(Guid listingId, Guid userId, CancellationToken ct = default);
}
