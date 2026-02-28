using EFCore.NamingConventions;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Minio;
using Rento.Application.Repositories;
using Rento.Application.Services;
using Rento.Infrastructure.Auth;
using Rento.Infrastructure.Data;
using Rento.Infrastructure.Redis;
using Rento.Infrastructure.Repositories;
using Rento.Infrastructure.Sms;
using StackExchange.Redis;

namespace Rento.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AddInfrastructure(this IServiceCollection services, IConfiguration configuration)
    {
        var connectionString = configuration.GetConnectionString("DefaultConnection")
            ?? configuration["ConnectionStrings:DefaultConnection"];
        if (!string.IsNullOrEmpty(connectionString))
        {
            services.AddDbContext<RentoDbContext>(options =>
            {
                options.UseNpgsql(connectionString, npgsql =>
                {
                    npgsql.MigrationsHistoryTable("__ef_migrations_history");
                });
                options.UseSnakeCaseNamingConvention();
            });
        }

        var redisConfig = configuration["Redis:Configuration"];
        if (!string.IsNullOrEmpty(redisConfig))
        {
            services.AddSingleton<IConnectionMultiplexer>(sp =>
            {
                var config = ConfigurationOptions.Parse(redisConfig);
                config.AbortOnConnectFail = false;
                return ConnectionMultiplexer.Connect(config);
            });
            services.AddSingleton<IOtpService, OtpService>();
            services.AddSingleton<ISessionService, SessionService>();
        }

        services.AddScoped<IJwtTokenService, JwtTokenService>();
        services.AddHttpClient();
        services.AddScoped<ISmsService, EskizSmsService>();
        services.AddScoped<IUserRepository, UserRepository>();
        services.AddScoped<IListingRepository, ListingRepository>();
        services.AddScoped<IFavoriteRepository, FavoriteRepository>();

        var minioEndpoint = configuration["MinIO:Endpoint"];
        var minioAccess = configuration["MinIO:AccessKey"];
        var minioSecret = configuration["MinIO:SecretKey"];
        var minioSsl = string.Equals(configuration["MinIO:UseSSL"], "true", StringComparison.OrdinalIgnoreCase);
        if (!string.IsNullOrEmpty(minioEndpoint) && !string.IsNullOrEmpty(minioAccess))
        {
            services.AddSingleton<IMinioClient>(sp => new MinioClient()
                .WithEndpoint(minioEndpoint)
                .WithCredentials(minioAccess, minioSecret)
                .WithSSL(minioSsl)
                .Build());
            services.AddScoped<Rento.Application.Services.IMediaService, Rento.Infrastructure.Media.MediaService>();
        }

        return services;
    }
}
