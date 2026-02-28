namespace Rento.Application.Repositories;

public interface IUserRepository
{
    Task<Rento.Core.Entities.User?> GetByIdAsync(Guid id, CancellationToken ct = default);
    Task<Rento.Core.Entities.User?> GetByPhoneAsync(string phone, CancellationToken ct = default);
    Task<Rento.Core.Entities.User> CreateAsync(Rento.Core.Entities.User user, CancellationToken ct = default);
    Task UpdateLastSeenAsync(Guid userId, CancellationToken ct = default);
    Task<bool> UpdateProfileAsync(Guid userId, string? fullName, string? email, string? language, CancellationToken ct = default);
}
