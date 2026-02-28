using Microsoft.EntityFrameworkCore;
using Rento.Core.Entities;

namespace Rento.Infrastructure.Data;

public class RentoDbContext : DbContext
{
    public RentoDbContext(DbContextOptions<RentoDbContext> options) : base(options) { }

    public DbSet<User> Users => Set<User>();
    public DbSet<Listing> Listings => Set<Listing>();
    public DbSet<ListingImage> ListingImages => Set<ListingImage>();
    public DbSet<ChatRoom> ChatRooms => Set<ChatRoom>();
    public DbSet<Message> Messages => Set<Message>();
    public DbSet<Favorite> Favorites => Set<Favorite>();
    public DbSet<Notification> Notifications => Set<Notification>();
    public DbSet<FcmToken> FcmTokens => Set<FcmToken>();
    public DbSet<Report> Reports => Set<Report>();
    public DbSet<SavedSearch> SavedSearches => Set<SavedSearch>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<User>(e =>
        {
            e.HasIndex(u => u.Phone).IsUnique();
        });

        modelBuilder.Entity<ChatRoom>(e =>
        {
            e.HasIndex(c => new { c.ListingId, c.TenantId, c.LandlordId }).IsUnique();
        });

        modelBuilder.Entity<Favorite>(e =>
        {
            e.HasIndex(f => new { f.UserId, f.ListingId }).IsUnique();
        });

        modelBuilder.Entity<FcmToken>(e =>
        {
            e.HasIndex(f => new { f.UserId, f.Token }).IsUnique();
        });

        modelBuilder.Entity<Report>(e =>
        {
            e.HasIndex(r => new { r.ReporterId, r.TargetType, r.TargetId }).IsUnique();
        });
    }
}
