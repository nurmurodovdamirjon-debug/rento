using Microsoft.EntityFrameworkCore;
using Rento.Application.Common;
using Rento.Application.Repositories;
using Rento.Core.Entities;
using Rento.Infrastructure.Data;

namespace Rento.Infrastructure.Repositories;

public class UserRepository(RentoDbContext db) : IUserRepository
{

    public async Task<User?> GetByIdAsync(Guid id, CancellationToken ct = default)
    {
        return await db.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == id, ct);
    }

    public async Task<User?> GetByPhoneAsync(string phone, CancellationToken ct = default)
    {
        return await db.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Phone == phone, ct);
    }

    public async Task<User> CreateAsync(User user, CancellationToken ct = default)
    {
        user.Id = Guid.NewGuid();
        user.CreatedAt = DateTime.UtcNow;
        user.UpdatedAt = DateTime.UtcNow;
        user.PhoneVerified = true;
        db.Users.Add(user);
        await db.SaveChangesAsync(ct);
        return user;
    }

    public async Task UpdateLastSeenAsync(Guid userId, CancellationToken ct = default)
    {
        await db.Users
            .Where(u => u.Id == userId)
            .ExecuteUpdateAsync(s => s.SetProperty(u => u.LastSeenAt, DateTime.UtcNow), ct);
    }

    public async Task<bool> UpdateProfileAsync(Guid userId, string? fullName, string? email, string? language, CancellationToken ct = default)
    {
        // Use ExecuteUpdateAsync to avoid loading entity into memory
        var updated = await db.Users
            .Where(u => u.Id == userId)
            .ExecuteUpdateAsync(s => s
                .SetProperty(u => u.FullName, u => fullName ?? u.FullName)
                .SetProperty(u => u.Email, u => email != null
                    ? (string.IsNullOrWhiteSpace(email) ? null : email)
                    : u.Email)
                .SetProperty(u => u.Language, u => language ?? u.Language)
                .SetProperty(u => u.UpdatedAt, DateTime.UtcNow), ct);
        return updated > 0;
    }

    public async Task<bool> SetBlockedAsync(Guid userId, bool blocked, CancellationToken ct = default)
    {
        var updated = await db.Users
            .Where(u => u.Id == userId)
            .ExecuteUpdateAsync(s => s.SetProperty(u => u.IsBlocked, blocked), ct);
        return updated > 0;
    }

    public async Task<PaginatedResult<User>> GetUsersAsync(int page, int perPage, CancellationToken ct = default)
    {
        var q = db.Users.AsNoTracking().OrderByDescending(u => u.CreatedAt);
        var total = await q.CountAsync(ct);
        var items = await q.Skip((page - 1) * perPage).Take(perPage).ToListAsync(ct);
        return PaginatedResult<User>.Create(items, page, perPage, total);
    }
}
