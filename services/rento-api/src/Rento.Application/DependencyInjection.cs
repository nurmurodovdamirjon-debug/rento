using FluentValidation;
using Microsoft.Extensions.DependencyInjection;
using Rento.Application.Services;
using Rento.Application.Services.Implementations;
using Rento.Application.Validators;

namespace Rento.Application;

public static class DependencyInjection
{
    public static IServiceCollection AddApplication(this IServiceCollection services)
    {
        services.AddScoped<IAuthService, AuthService>();
        services.AddScoped<IUserService, UserService>();
        services.AddScoped<IListingService, ListingService>();
        services.AddScoped<IFavoriteService, FavoriteService>();
        services.AddScoped<IChatService, ChatService>();
        services.AddScoped<INotificationService, NotificationService>();
        services.AddScoped<IReportService, ReportService>();
        services.AddValidatorsFromAssemblyContaining<SendOtpRequestValidator>();
        return services;
    }
}
