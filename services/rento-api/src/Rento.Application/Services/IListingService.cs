using Rento.Application.Common;
using Rento.Application.DTOs.Listings;

namespace Rento.Application.Services;

/// <summary>
/// Listing CRUD, search and filtering service.
/// </summary>
public interface IListingService
{
    /// <summary>Gets active listings with optional filters.</summary>
    Task<PaginatedResult<ListingSummaryDto>> GetListingsAsync(ListingFilter filter, CancellationToken ct = default);

    /// <summary>Full-text search over listings.</summary>
    Task<PaginatedResult<ListingSummaryDto>> SearchAsync(SearchFilter filter, CancellationToken ct = default);

    /// <summary>Listings near a point (PostGIS placeholder).</summary>
    Task<PaginatedResult<ListingSummaryDto>> GetNearbyAsync(NearbyFilter filter, CancellationToken ct = default);

    /// <summary>Gets listing by id with images; returns null if not found.</summary>
    Task<ListingDetailDto?> GetByIdAsync(Guid id, Guid? userId, CancellationToken ct = default);

    /// <summary>Listings owned by the user, optionally filtered by status.</summary>
    Task<PaginatedResult<ListingSummaryDto>> GetMyListingsAsync(Guid userId, string? status, int page, int perPage, CancellationToken ct = default);

    /// <summary>Creates a listing; returns null if daily limit exceeded.</summary>
    Task<ListingDetailDto?> CreateAsync(Guid userId, CreateListingRequest request, CancellationToken ct = default);

    /// <summary>Updates listing; returns null if not found or not owner.</summary>
    Task<ListingDetailDto?> UpdateAsync(Guid listingId, Guid userId, UpdateListingRequest request, CancellationToken ct = default);

    /// <summary>Deletes listing; returns false if not found or not owner.</summary>
    Task<bool> DeleteAsync(Guid listingId, Guid userId, CancellationToken ct = default);

    /// <summary>Updates listing status (e.g. active, paused).</summary>
    Task<bool> UpdateStatusAsync(Guid listingId, Guid userId, string status, CancellationToken ct = default);

    /// <summary>Returns view/favorite/contact counts for the listing owner.</summary>
    Task<ListingStatsDto?> GetStatsAsync(Guid listingId, Guid userId, CancellationToken ct = default);
}
