using Rento.Application.Common;
using Rento.Application.DTOs.Listings;
using Rento.Application.Mappers;
using Rento.Application.Repositories;

namespace Rento.Application.Services.Implementations;

public class ListingService(IListingRepository listingRepo) : IListingService
{
    private const int DailyCreateLimit = 10;

    public async Task<PaginatedResult<ListingSummaryDto>> GetListingsAsync(ListingFilter filter, CancellationToken ct = default)
    {
        var result = await listingRepo.GetListingsAsync(filter, ct);
        return PaginatedResult<ListingSummaryDto>.Create(
            result.Items.Select(ListingMapper.ToSummary).ToList(),
            result.Page,
            result.PerPage,
            result.Total);
    }

    public async Task<PaginatedResult<ListingSummaryDto>> SearchAsync(SearchFilter filter, CancellationToken ct = default)
    {
        var result = await listingRepo.SearchListingsAsync(filter, ct);
        return PaginatedResult<ListingSummaryDto>.Create(
            result.Items.Select(ListingMapper.ToSummary).ToList(),
            result.Page,
            result.PerPage,
            result.Total);
    }

    public Task<PaginatedResult<ListingSummaryDto>> GetNearbyAsync(NearbyFilter filter, CancellationToken ct = default)
    {
        // TODO: PostGIS
        return Task.FromResult(PaginatedResult<ListingSummaryDto>.Create([], filter.Page, filter.PerPage, 0));
    }

    public async Task<ListingDetailDto?> GetByIdAsync(Guid id, Guid? userId, CancellationToken ct = default)
    {
        var listing = await listingRepo.GetByIdWithImagesAsync(id, ct);
        return listing == null ? null : ListingMapper.ToDetail(listing);
    }

    public async Task<PaginatedResult<ListingSummaryDto>> GetMyListingsAsync(Guid userId, string? status, int page, int perPage, CancellationToken ct = default)
    {
        var result = await listingRepo.GetMyListingsAsync(userId, status, page, perPage, ct);
        return PaginatedResult<ListingSummaryDto>.Create(
            result.Items.Select(ListingMapper.ToSummary).ToList(),
            result.Page,
            result.PerPage,
            result.Total);
    }

    public async Task<ListingDetailDto?> CreateAsync(Guid userId, CreateListingRequest request, CancellationToken ct = default)
    {
        var todayCount = await listingRepo.CountCreatedTodayAsync(userId, ct);
        if (todayCount >= DailyCreateLimit)
            return null;

        var listing = ListingMapper.ToEntity(userId, request);
        var created = await listingRepo.CreateAsync(listing, ct);
        return await GetByIdAsync(created.Id, userId, ct);
    }

    public async Task<ListingDetailDto?> UpdateAsync(Guid listingId, Guid userId, UpdateListingRequest request, CancellationToken ct = default)
    {
        var existing = await listingRepo.GetByIdAsync(listingId, ct);
        if (existing == null || existing.UserId != userId) return null;

        ListingMapper.ApplyUpdate(existing, request);

        await listingRepo.UpdateAsync(existing, ct);
        return await GetByIdAsync(listingId, userId, ct);
    }

    public Task<bool> DeleteAsync(Guid listingId, Guid userId, CancellationToken ct = default)
    {
        return listingRepo.DeleteAsync(listingId, userId, ct);
    }

    public async Task<bool> UpdateStatusAsync(Guid listingId, Guid userId, string status, CancellationToken ct = default)
    {
        return await listingRepo.UpdateStatusAsync(listingId, userId, status, ct);
    }

    public async Task<ListingStatsDto?> GetStatsAsync(Guid listingId, Guid userId, CancellationToken ct = default)
    {
        var listing = await listingRepo.GetByIdAsync(listingId, ct);
        if (listing == null || listing.UserId != userId) return null;
        return new ListingStatsDto { Views = listing.ViewsCount, Favorites = listing.FavoritesCount, Contacts = listing.ContactsCount };
    }
}
