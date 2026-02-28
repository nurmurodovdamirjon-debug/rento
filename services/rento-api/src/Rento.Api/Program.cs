using System.Text;
using System.Text.Json;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using Microsoft.Extensions.Diagnostics.HealthChecks;
using Microsoft.IdentityModel.Tokens;
using Prometheus;
using Rento.Api.Middleware;
using Rento.Application;
using Rento.Infrastructure;
using Serilog;
using StackExchange.Redis;

var builder = WebApplication.CreateBuilder(args);

// Serilog
builder.Host.UseSerilog((ctx, lc) =>
{
    lc.ReadFrom.Configuration(ctx.Configuration)
      .Enrich.FromLogContext()
      .WriteTo.Console();
});

// Configuration
var config = builder.Configuration;

// Infrastructure
builder.Services.AddInfrastructure(config);

// Application
builder.Services.AddApplication();

// JWT Bearer
var jwtSecret = config["Jwt:AccessSecret"];
if (!string.IsNullOrEmpty(jwtSecret))
{
    builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
        .AddJwtBearer(options =>
        {
            options.TokenValidationParameters = new TokenValidationParameters
            {
                ValidateIssuerSigningKey = true,
                IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSecret)),
                ValidIssuer = "rento.uz",
                ValidateIssuer = true,
                ValidateAudience = false,
                ValidateLifetime = true,
                ClockSkew = TimeSpan.Zero
            };
            options.Events = new Microsoft.AspNetCore.Authentication.JwtBearer.JwtBearerEvents
            {
                OnMessageReceived = ctx =>
                {
                    var accessToken = ctx.Request.Query["access_token"];
                    var path = ctx.HttpContext.Request.Path;
                    if (!string.IsNullOrEmpty(accessToken) && path.StartsWithSegments("/hubs"))
                        ctx.Token = accessToken;
                    return Task.CompletedTask;
                }
            };
        });
    builder.Services.AddAuthorization();
}

// Health checks
var healthBuilder = builder.Services.AddHealthChecks();
var connStr = config.GetConnectionString("DefaultConnection");
if (!string.IsNullOrEmpty(connStr))
    healthBuilder.AddNpgSql(connStr, name: "postgres");
var redisConfig = config["Redis:Configuration"];
if (!string.IsNullOrEmpty(redisConfig))
    healthBuilder.AddRedis(redisConfig, name: "redis");
var esUrl = config["Elasticsearch:Url"];
if (!string.IsNullOrEmpty(esUrl))
    healthBuilder.AddElasticsearch(esUrl, name: "elasticsearch");

// CORS
var allowedOrigins = config.GetSection("Cors:AllowedOrigins").Get<string[]>() ?? Array.Empty<string>();
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        if (allowedOrigins.Length > 0)
            policy.WithOrigins(allowedOrigins).AllowAnyMethod().AllowAnyHeader();
        else
            policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader();
    });
});

// Controllers with JSON snake_case
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower;
        options.JsonSerializerOptions.PropertyNameCaseInsensitive = true;
    });

// Swagger
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new() { Title = "Rento API", Version = "v1" });
});

builder.Services.AddSignalR();

var app = builder.Build();

// Global exception handler
app.UseGlobalExceptionHandler();

// CORS
app.UseCors();

// Rate limit (Redis)
app.UseRedisRateLimit();

// Authentication & Authorization
app.UseAuthentication();
app.UseAuthorization();

// Prometheus metrics
app.UseHttpMetrics();

// Swagger — enable in all environments; use config to restrict in production if needed
app.UseSwagger();
app.UseSwaggerUI(c =>
{
    c.SwaggerEndpoint("/swagger/v1/swagger.json", "Rento API v1");
    c.RoutePrefix = "swagger"; // UI at /swagger
});

// Redirect root to Swagger UI
app.MapGet("/", () => Results.Redirect("/swagger")).ExcludeFromDescription();

// Health
app.MapHealthChecks("/health", new HealthCheckOptions
{
    ResponseWriter = HealthCheckResponseWriter.WriteResponse
});

// Metrics
app.MapMetrics();

// API v1 (controllers use [Route("api/v1/...")])
app.MapControllers();
app.MapHub<Rento.Api.Hubs.ChatHub>("/hubs/chat");

app.Run();
