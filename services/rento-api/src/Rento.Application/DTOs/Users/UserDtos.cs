namespace Rento.Application.DTOs.Users;

public class UserProfileDto
{
    public Guid Id { get; set; }
    public string Phone { get; set; } = "";
    public string? FullName { get; set; }
    public string? Email { get; set; }
    public string? AvatarUrl { get; set; }
    public string Role { get; set; } = "";
    public string Language { get; set; } = "uz";
    public DateTime? LastSeenAt { get; set; }
    public DateTime CreatedAt { get; set; }
}

public class UpdateProfileRequest
{
    public string? FullName { get; set; }
    public string? Email { get; set; }
    public string? Language { get; set; }
}
