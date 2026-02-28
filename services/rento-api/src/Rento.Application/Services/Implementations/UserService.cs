using Rento.Application.DTOs.Users;
using Rento.Application.Repositories;
using Rento.Core.Entities;

namespace Rento.Application.Services.Implementations;

public class UserService : IUserService
{
    private readonly IUserRepository _userRepo;

    public UserService(IUserRepository userRepo)
    {
        _userRepo = userRepo;
    }

    public async Task<UserProfileDto?> GetMeAsync(Guid userId, CancellationToken ct = default)
    {
        var user = await _userRepo.GetByIdAsync(userId, ct);
        return user == null ? null : MapToProfile(user);
    }

    public async Task<UserProfileDto?> GetByIdAsync(Guid id, CancellationToken ct = default)
    {
        var user = await _userRepo.GetByIdAsync(id, ct);
        return user == null ? null : MapToProfile(user);
    }

    public async Task<UserProfileDto?> UpdateMeAsync(Guid userId, UpdateProfileRequest request, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(request.FullName) && string.IsNullOrWhiteSpace(request.Email) && string.IsNullOrWhiteSpace(request.Language))
            return null;

        var ok = await _userRepo.UpdateProfileAsync(
            userId,
            string.IsNullOrWhiteSpace(request.FullName) ? null : request.FullName.Trim(),
            request.Email != null ? (string.IsNullOrWhiteSpace(request.Email) ? null : request.Email.Trim()) : null,
            string.IsNullOrWhiteSpace(request.Language) ? null : request.Language.Trim(),
            ct);
        if (!ok) return null;
        return await GetMeAsync(userId, ct);
    }

    private static UserProfileDto MapToProfile(User user)
    {
        return new UserProfileDto
        {
            Id = user.Id,
            Phone = user.Phone,
            FullName = user.FullName,
            Email = user.Email,
            AvatarUrl = user.AvatarUrl,
            Role = user.Role,
            Language = user.Language,
            LastSeenAt = user.LastSeenAt,
            CreatedAt = user.CreatedAt
        };
    }
}
