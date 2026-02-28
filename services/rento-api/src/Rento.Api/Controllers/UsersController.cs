using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rento.Application.DTOs.Users;
using Rento.Application.Services;
using Rento.Core.Common;

namespace Rento.Api.Controllers;

[ApiController]
[Route("api/v1/users")]
[Authorize]
public class UsersController : ControllerBase
{
    private readonly IUserService _userService;

    public UsersController(IUserService userService)
    {
        _userService = userService;
    }

    private Guid? GetUserId()
    {
        var sub = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub");
        return Guid.TryParse(sub, out var id) ? id : null;
    }

    [HttpGet("me")]
    public async Task<IActionResult> GetMe(CancellationToken ct)
    {
        var userId = GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });

        var profile = await _userService.GetMeAsync(userId.Value, ct);
        if (profile == null)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.UserNotFound, Message = "User not found" } });

        return Ok(new ApiResponse { Success = true, Data = profile });
    }

    [HttpPut("me")]
    public async Task<IActionResult> UpdateMe([FromBody] UpdateProfileRequest request, CancellationToken ct)
    {
        var userId = GetUserId();
        if (userId == null)
            return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });

        if (string.IsNullOrWhiteSpace(request?.FullName) && string.IsNullOrWhiteSpace(request?.Email) && string.IsNullOrWhiteSpace(request?.Language))
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.NoFields, Message = "At least one field required" } });

        var profile = await _userService.UpdateMeAsync(userId.Value, request!, ct);
        if (profile == null)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.UserNotFound, Message = "User not found" } });

        return Ok(new ApiResponse { Success = true, Data = profile });
    }

    [HttpGet("{id}")]
    [AllowAnonymous]
    public async Task<IActionResult> GetById(string id, CancellationToken ct)
    {
        if (string.Equals(id, "me", StringComparison.OrdinalIgnoreCase))
        {
            var userId = GetUserId();
            if (userId == null)
                return Unauthorized(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.AuthRequired, Message = "Unauthorized" } });
            var profile = await _userService.GetMeAsync(userId.Value, ct);
            if (profile == null)
                return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.UserNotFound, Message = "User not found" } });
            return Ok(new ApiResponse { Success = true, Data = profile });
        }

        if (!Guid.TryParse(id, out var guid))
            return BadRequest(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.InvalidId, Message = "Invalid user id" } });

        var user = await _userService.GetByIdAsync(guid, ct);
        if (user == null)
            return NotFound(new ApiResponse { Success = false, Error = new ApiError { Code = ErrorCodes.UserNotFound, Message = "User not found" } });

        return Ok(new ApiResponse { Success = true, Data = user });
    }
}
