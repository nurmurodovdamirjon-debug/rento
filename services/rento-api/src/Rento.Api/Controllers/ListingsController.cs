using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Api.Extensions;
using Rento.Application.DTOs.Listings;
using Rento.Application.Services;
using Rento.Core.Common;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/listings")]
public class ListingsController : ControllerBase
{
    private readonly IListingService _listingService;

    public ListingsController(IListingService listingService)
    {
        _listingService = listingService;
    }

    [HttpGet]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetListings([FromQuery] ListingFilter filter, CancellationToken ct)
    {
        var result = await _listingService.GetListingsAsync(filter, ct);
        return Ok(new ApiResponse
        {
            Success = true,
            Data = new { items = result.Items, meta = new { result.Page, result.PerPage, result.Total, result.TotalPages } }
        });
    }

    [HttpGet("search")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> Search([FromQuery] SearchFilter filter, CancellationToken ct)
    {
        var result = await _listingService.SearchAsync(filter, ct);
        return Ok(new ApiResponse
        {
            Success = true,
            Data = new { items = result.Items, meta = new { result.Page, result.PerPage, result.Total, result.TotalPages } }
        });
    }

    [HttpGet("nearby")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetNearby([FromQuery] NearbyFilter filter, CancellationToken ct)
    {
        var result = await _listingService.GetNearbyAsync(filter, ct);
        return Ok(new ApiResponse
        {
            Success = true,
            Data = new { items = result.Items, meta = new { result.Page, result.PerPage, result.Total, result.TotalPages } }
        });
    }

    [HttpGet("my")]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> GetMy([FromQuery] string? status, [FromQuery] int page = 1, [FromQuery] int per_page = 20, CancellationToken ct = default)
    {
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
        var result = await _listingService.GetMyListingsAsync(userId.Value, status, page, per_page, ct);
        return Ok(new ApiResponse
        {
            Success = true,
            Data = new { items = result.Items, meta = new { result.Page, per_page = result.PerPage, result.Total, result.TotalPages } }
        });
    }

    [HttpGet("{id}")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(string id, CancellationToken ct)
    {
        if (string.Equals(id, "my", StringComparison.OrdinalIgnoreCase))
            return RedirectToAction(nameof(GetMy));
        if (!Guid.TryParse(id, out var guid))
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.InvalidId, Message = "Invalid id" } });
        var userId = User.GetUserId();
        var listing = await _listingService.GetByIdAsync(guid, userId, ct);
        if (listing == null)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ListingNotFound, Message = "Listing not found" } });
        return Ok(new ApiResponse { Success = true, Data = listing });
    }

    [HttpPost]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status429TooManyRequests)]
    public async Task<IActionResult> Create([FromBody] CreateListingRequest? request, CancellationToken ct)
    {
        if (request is null)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "request body is required" } });

        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
        var listing = await _listingService.CreateAsync(userId.Value, request, ct);
        if (listing == null)
            return StatusCode(429, new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.RateLimitExceeded, Message = "Daily listing limit exceeded" } });
        return StatusCode(201, new ApiResponse { Success = true, Data = listing });
    }

    [HttpPut("{id}")]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Update(Guid id, [FromBody] UpdateListingRequest? request, CancellationToken ct)
    {
        if (request is null)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "request body is required" } });

        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
        var listing = await _listingService.UpdateAsync(id, userId.Value, request, ct);
        if (listing == null)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ListingNotFound, Message = "Listing not found or not owner" } });
        return Ok(new ApiResponse { Success = true, Data = listing });
    }

    [HttpDelete("{id}")]
    [Authorize]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Delete(Guid id, CancellationToken ct)
    {
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
        var ok = await _listingService.DeleteAsync(id, userId.Value, ct);
        if (!ok)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ListingNotFound, Message = "Listing not found or not owner" } });
        return NoContent();
    }

    [HttpPut("{id}/status")]
    [Authorize]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> UpdateStatus(Guid id, [FromBody] UpdateStatusRequest? body, CancellationToken ct)
    {
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
        if (string.IsNullOrWhiteSpace(body?.Status))
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "status is required" } });
        var ok = await _listingService.UpdateStatusAsync(id, userId.Value, body.Status, ct);
        if (!ok)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.InvalidStatusTransition, Message = "Invalid status or not owner" } });
        return NoContent();
    }

    [HttpGet("{id}/stats")]
    [Authorize]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetStats(Guid id, CancellationToken ct)
    {
        var userId = User.GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
        var stats = await _listingService.GetStatsAsync(id, userId.Value, ct);
        if (stats == null)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ListingNotFound, Message = "Listing not found or not owner" } });
        return Ok(new ApiResponse { Success = true, Data = stats });
    }
}

public class UpdateStatusRequest
{
    public required string Status { get; init; }
}
