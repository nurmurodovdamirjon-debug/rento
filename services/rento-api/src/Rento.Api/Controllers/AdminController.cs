using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Application.Repositories;
using Rento.Core.Common;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/admin")]
[Authorize(Roles = UserRole.Admin)]
public class AdminController : ControllerBase
{
    private readonly IUserRepository _userRepo;
    private readonly IListingRepository _listingRepo;

    public AdminController(IUserRepository userRepo, IListingRepository listingRepo)
    {
        _userRepo = userRepo;
        _listingRepo = listingRepo;
    }

    [HttpGet("users")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetUsers([FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20, CancellationToken ct = default)
    {
        var result = await _userRepo.GetUsersAsync(page, perPage, ct);
        var items = result.Items.Select(u => new { id = u.Id, phone = u.Phone, full_name = u.FullName, role = u.Role, is_blocked = u.IsBlocked, is_active = u.IsActive, created_at = u.CreatedAt }).ToList();
        return Ok(new ApiResponse { Success = true, Data = new { items, meta = new { result.Page, per_page = result.PerPage, total = result.Total, total_pages = result.TotalPages } } });
    }

    [HttpPut("users/{id}/block")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> BlockUser(Guid id, CancellationToken ct = default)
    {
        var ok = await _userRepo.SetBlockedAsync(id, true, ct);
        if (!ok) return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.UserNotFound, Message = "User not found" } });
        return NoContent();
    }

    [HttpPut("users/{id}/unblock")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> UnblockUser(Guid id, CancellationToken ct = default)
    {
        var ok = await _userRepo.SetBlockedAsync(id, false, ct);
        if (!ok) return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.UserNotFound, Message = "User not found" } });
        return NoContent();
    }

    [HttpGet("listings/pending")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetPendingListings([FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20, CancellationToken ct = default)
    {
        var result = await _listingRepo.GetPendingListingsAsync(page, perPage, ct);
        var items = result.Items.Select(l => new
        {
            id = l.Id,
            user_id = l.UserId,
            title = l.Title,
            city = l.City,
            price = l.Price,
            status = l.Status,
            created_at = l.CreatedAt,
            image_url = l.ListingImages?.Where(i => i.IsMain).Select(i => i.Url).FirstOrDefault()
        }).ToList();
        return Ok(new ApiResponse { Success = true, Data = new { items, meta = new { result.Page, per_page = result.PerPage, total = result.Total, total_pages = result.TotalPages } } });
    }

    [HttpPut("listings/{id}/approve")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> ApproveListing(Guid id, CancellationToken ct = default)
    {
        var ok = await _listingRepo.UpdateStatusByAdminAsync(id, ListingStatus.Active, null, ct);
        if (!ok) return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ListingNotFound, Message = "Listing not found" } });
        return NoContent();
    }

    [HttpPut("listings/{id}/reject")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> RejectListing(Guid id, [FromBody] RejectRequest? body, CancellationToken ct = default)
    {
        var ok = await _listingRepo.UpdateStatusByAdminAsync(id, ListingStatus.Rejected, body?.RejectionReason, ct);
        if (!ok) return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ListingNotFound, Message = "Listing not found" } });
        return NoContent();
    }
}

public class RejectRequest
{
    public string? RejectionReason { get; init; }
}
