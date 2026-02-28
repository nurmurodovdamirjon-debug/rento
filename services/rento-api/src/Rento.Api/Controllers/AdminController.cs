using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Core.Common;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/admin")]
[Authorize(Roles = "admin")]
public class AdminController : ControllerBase
{
    [HttpGet("users")]
    public IActionResult GetUsers([FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20)
    {
        return Ok(new ApiResponse { Success = true, Data = new { items = Array.Empty<object>(), meta = new { page, per_page = perPage, total = 0, total_pages = 0 } } });
    }

    [HttpPut("users/{id}/block")]
    public IActionResult BlockUser(Guid id) => NoContent();

    [HttpPut("users/{id}/unblock")]
    public IActionResult UnblockUser(Guid id) => NoContent();

    [HttpGet("listings/pending")]
    public IActionResult GetPendingListings([FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20)
    {
        return Ok(new ApiResponse { Success = true, Data = new { items = Array.Empty<object>(), meta = new { page, per_page = perPage, total = 0, total_pages = 0 } } });
    }

    [HttpPut("listings/{id}/approve")]
    public IActionResult ApproveListing(Guid id) => NoContent();

    [HttpPut("listings/{id}/reject")]
    public IActionResult RejectListing(Guid id, [FromBody] RejectRequest? body) => NoContent();
}

public class RejectRequest
{
    public string? RejectionReason { get; set; }
}
