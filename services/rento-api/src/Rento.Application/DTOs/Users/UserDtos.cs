using Rento.Core.Common;

namespace Rento.Application.DTOs.Users;

public class UserProfileDto
{
    public Guid Id { get; init; }
    public required string Phone { get; init; }
    public string? FullName { get; init; }
    public string? Email { get; init; }
    public string? AvatarUrl { get; init; }
    public required string Role { get; init; }
    public string Language { get; init; } = UserLanguage.Uzbek;
    public DateTime? LastSeenAt { get; init; }
    public DateTime CreatedAt { get; init; }
}

public class UpdateProfileRequest
{
    public string? FullName { get; init; }
    public string? Email { get; init; }
    public string? Language { get; init; }
}
