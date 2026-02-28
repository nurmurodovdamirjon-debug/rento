using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Core.Common;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/reports")]
[Authorize]
public class ReportsController : ControllerBase
{
    [HttpPost]
    public IActionResult Create([FromBody] CreateReportRequest? body)
    {
        if (body == null)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "Invalid body" } });
        return StatusCode(201, new ApiResponse { Success = true, Data = new { id = Guid.NewGuid() } });
    }
}

[ApiController]
[Route("api/v1/reports/admin")]
[Authorize(Roles = "admin")]
public class ReportsAdminController : ControllerBase
{
    [HttpGet]
    public IActionResult GetList([FromQuery] string? status, [FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20)
    {
        return Ok(new ApiResponse { Success = true, Data = new { items = Array.Empty<object>(), meta = new { page, per_page = perPage, total = 0, total_pages = 0 } } });
    }

    [HttpGet("{id}")]
    public IActionResult GetById(Guid id)
    {
        return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ReportNotFound, Message = "Not found" } });
    }

    [HttpPut("{id}/resolve")]
    public IActionResult Resolve(Guid id, [FromBody] ResolveReportRequest? body) => NoContent();
}

public class CreateReportRequest
{
    public string? TargetType { get; set; }
    public Guid? TargetId { get; set; }
    public string? Reason { get; set; }
    public string? Description { get; set; }
}

public class ResolveReportRequest
{
    public string? Status { get; set; }
    public string? AdminNote { get; set; }
}
