using Rento.Application.Common;
using Rento.Core.Entities;

namespace Rento.Application.Repositories;

public interface IUserRepository
{
    Task<User?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<User?> GetByPhoneAsync(string phone, CancellationToken ct = default);
    Task<User> CreateAsync(User user, CancellationToken ct = default);
    Task UpdateLastSeenAsync(Guid userId, CancellationToken ct = default);
    Task<bool> UpdateProfileAsync(Guid userId, string? fullName, string? email, string? language, CancellationToken ct = default);
    Task<bool> SetBlockedAsync(Guid userId, bool blocked, CancellationToken ct = default);
    Task<PaginatedResult<User>> GetUsersAsync(int page, int perPage, CancellationToken ct = default);
}
