using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Application.Services;
using Rento.Core.Common;
using Rento.Api.Extensions;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/reports")]
[Authorize]
public class ReportsController : ControllerBase
{
    private readonly IReportService _reportService;

    public ReportsController(IReportService reportService)
    {
        _reportService = reportService;
    }

    [HttpPost]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status201Created)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    public async Task<IActionResult> Create([FromBody] CreateReportRequest? body, CancellationToken ct = default)
    {
        if (body == null || string.IsNullOrWhiteSpace(body.TargetType) || body.TargetId == null || string.IsNullOrWhiteSpace(body.Reason))
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ValidationError, Message = "target_type, target_id and reason required" } });
        var userId = User.GetUserId();
        if (userId == null) return Unauthorized();
        var result = await _reportService.CreateAsync(userId.Value, body.TargetType, body.TargetId.Value, body.Reason, body.Description, ct);
        if (result == null)
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AlreadyReported, Message = "Already reported this target" } });
        return StatusCode(201, new ApiResponse { Success = true, Data = new { id = result.Id } });
    }
}

[ApiController]
[Route("api/v1/reports/admin")]
[Authorize(Roles = UserRole.Admin)]
public class ReportsAdminController : ControllerBase
{
    private readonly IReportService _reportService;

    public ReportsAdminController(IReportService reportService)
    {
        _reportService = reportService;
    }

    [HttpGet]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    public async Task<IActionResult> GetList([FromQuery] string? status, [FromQuery] int page = 1, [FromQuery(Name = "per_page")] int perPage = 20, CancellationToken ct = default)
    {
        var result = await _reportService.GetListAsync(status, page, perPage, ct);
        return Ok(new ApiResponse { Success = true, Data = new { items = result.Items, meta = new { result.Page, per_page = result.PerPage, total = result.Total, total_pages = result.TotalPages } } });
    }

    [HttpGet("{id}")]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status200OK)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetById(Guid id, CancellationToken ct = default)
    {
        var report = await _reportService.GetByIdAsync(id, ct);
        if (report == null)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ReportNotFound, Message = "Not found" } });
        return Ok(new ApiResponse { Success = true, Data = report });
    }

    [HttpPut("{id}/resolve")]
    [ProducesResponseType(StatusCodes.Status204NoContent)]
    [ProducesResponseType(StatusCodes.Status401Unauthorized)]
    [ProducesResponseType(typeof(ApiResponse), StatusCodes.Status404NotFound)]
    public async Task<IActionResult> Resolve(Guid id, [FromBody] ResolveReportRequest? body, CancellationToken ct = default)
    {
        var adminId = User.GetUserId();
        if (adminId == null) return Unauthorized();
        var ok = await _reportService.ResolveAsync(id, body?.Status ?? ReportStatus.Resolved, body?.AdminNote, adminId.Value, ct);
        if (!ok)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.ReportNotFound, Message = "Not found" } });
        return NoContent();
    }
}

public class CreateReportRequest
{
    public string? TargetType { get; init; }
    public Guid? TargetId { get; init; }
    public string? Reason { get; init; }
    public string? Description { get; init; }
}

public class ResolveReportRequest
{
    public string? Status { get; init; }
    public string? AdminNote { get; init; }
}
