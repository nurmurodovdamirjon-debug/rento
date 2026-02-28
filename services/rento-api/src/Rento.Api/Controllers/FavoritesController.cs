using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Application.Services;
using Rento.Core.Common;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/favorites")]
[Authorize]
public class FavoritesController : ControllerBase
{
    private readonly IFavoriteService _favoriteService;

    public FavoritesController(IFavoriteService favoriteService) => _favoriteService = favoriteService;

    private Guid? GetUserId()
    {
        var sub = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub");
        return Guid.TryParse(sub, out var id) ? id : null;
    }

    [HttpPost("{listingId}")]
    public async Task<IActionResult> Toggle(Guid listingId, CancellationToken ct)
    {
        var userId = GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
        var result = await _favoriteService.ToggleAsync(userId.Value, listingId, ct);
        return Ok(new ApiResponse { Success = true, Data = new { is_favorite = result.IsFavorite } });
    }

    [HttpGet]
    public async Task<IActionResult> GetList([FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20, CancellationToken ct = default)
    {
        var userId = GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
        var result = await _favoriteService.GetListAsync(userId.Value, page, perPage, ct);
        return Ok(new ApiResponse
        {
            Success = true,
            Data = new { items = result.Items, meta = new { result.Page, per_page = result.PerPage, result.Total, result.TotalPages } }
        });
    }

    [HttpGet("check/{listingId}")]
    public async Task<IActionResult> Check(Guid listingId, CancellationToken ct)
    {
        var userId = GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
        var isFav = await _favoriteService.CheckAsync(userId.Value, listingId, ct);
        return Ok(new ApiResponse { Success = true, Data = new { is_favorite = isFav } });
    }

    [HttpGet("ids")]
    public async Task<IActionResult> GetIds(CancellationToken ct)
    {
        var userId = GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
        var ids = await _favoriteService.GetIdsAsync(userId.Value, ct);
        return Ok(new ApiResponse { Success = true, Data = new { listing_ids = ids } });
    }
}
