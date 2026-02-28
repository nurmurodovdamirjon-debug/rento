using Rento.Application.DTOs.Users;

namespace Rento.Application.Services;

public interface IUserService
{
    Task<UserProfileDto?> GetMeAsync(Guid userId, CancellationToken ct = default);
    Task<UserProfileDto?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<UserProfileDto?> UpdateMeAsync(Guid userId, UpdateProfileRequest request, CancellationToken ct = default);
}
