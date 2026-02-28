using Microsoft.EntityFrameworkCore;
using Rento.Application.Repositories;
using Rento.Core.Entities;
using Rento.Infrastructure.Data;

namespace Rento.Infrastructure.Repositories;

public class UserRepository : IUserRepository
{
    private readonly RentoDbContext _db;

    public UserRepository(RentoDbContext db)
    {
        _db = db;
    }

    public async Task<User?> GetByIdAsync(Guid id, CancellationToken ct = default)
    {
        return await _db.Users.AsNoTracking().FirstOrDefaultAsync(u => u.Id == id, ct);
    }

    public async Task<User?> GetByPhoneAsync(string phone, CancellationToken ct = default)
    {
        return await _db.Users.FirstOrDefaultAsync(u => u.Phone == phone, ct);
    }

    public async Task<User> CreateAsync(User user, CancellationToken ct = default)
    {
        user.Id = Guid.NewGuid();
        user.CreatedAt = DateTime.UtcNow;
        user.UpdatedAt = DateTime.UtcNow;
        user.PhoneVerified = true;
        _db.Users.Add(user);
        await _db.SaveChangesAsync(ct);
        return user;
    }

    public async Task UpdateLastSeenAsync(Guid userId, CancellationToken ct = default)
    {
        await _db.Users
            .Where(u => u.Id == userId)
            .ExecuteUpdateAsync(s => s.SetProperty(u => u.LastSeenAt, DateTime.UtcNow), ct);
    }

    public async Task<bool> UpdateProfileAsync(Guid userId, string? fullName, string? email, string? language, CancellationToken ct = default)
    {
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Id == userId, ct);
        if (user == null) return false;
        if (fullName != null) user.FullName = fullName;
        if (email != null) user.Email = string.IsNullOrWhiteSpace(email) ? null : email;
        if (language != null) user.Language = language;
        await _db.SaveChangesAsync(ct);
        return true;
    }
}
