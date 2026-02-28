# Rento API (ASP.NET Core 8)

Monolithic backend for Rento, replacing the previous Go (core-api) + Node.js (auth-service, chat-service) stack.  
API contracts match `docs/RENTO_BACKEND_ANALYSIS.md` so the Android app can keep using the same base URL and payloads.

## Structure

- **Rento.Api** — Web API entry point, controllers, middleware (JWT, CORS, global exception, health, metrics).
- **Rento.Application** — Application services (Auth, User, Listing, …), DTOs, validators (FluentValidation).
- **Rento.Infrastructure** — EF Core (PostgreSQL), Redis (OTP, sessions), MinIO, Eskiz SMS, JWT, repositories.
- **Rento.Core** — Entities, shared API response/error types, error codes.

## Prerequisites

- .NET 8 SDK
- PostgreSQL (schema from repo `migrations/` or EF migrations)
- Redis
- MinIO (optional; for media upload)
- Elasticsearch (optional; for listing search)

## Configuration

Copy or override via environment or `appsettings.Development.json`:

- `ConnectionStrings:DefaultConnection` — PostgreSQL
- `Redis:Configuration` — e.g. `localhost:6379`
- `Jwt:AccessSecret`, `Jwt:RefreshSecret` — min 32 chars for HMAC
- `MinIO:Endpoint`, `MinIO:AccessKey`, `MinIO:SecretKey`, `MinIO:UseSSL`
- `Eskiz:Email`, `Eskiz:Password`, `Eskiz:BaseUrl` — for SMS OTP (dev logs OTP to console if not set)
- `Cors:AllowedOrigins` — array of allowed origins

## Run

```bash
cd services/rento-api
dotnet run --project src/Rento.Api/Rento.Api.csproj
```

Stop any running instance before building to avoid file lock errors.

## Endpoints (implemented)

- **Auth:** `POST /api/v1/auth/send-otp`, `verify-otp`, `refresh-token`, `POST /api/v1/auth/logout` (Bearer).
- **Users:** `GET/PUT /api/v1/users/me`, `GET /api/v1/users/:id`.
- **Listings:** `GET /api/v1/listings`, `GET /api/v1/listings/search`, `GET /api/v1/listings/nearby`, `GET /api/v1/listings/my`, `GET/POST/PUT/DELETE /api/v1/listings/:id`, `PUT /api/v1/listings/:id/status`, `GET /api/v1/listings/:id/stats`.
- **Media:** `POST /api/v1/media/upload` (multipart: file, bucket).
- **Favorites:** `POST /api/v1/favorites/:listing_id` (toggle), `GET /api/v1/favorites`, `GET /api/v1/favorites/check/:listing_id`, `GET /api/v1/favorites/ids`.
- **Notifications:** `GET /api/v1/notifications`, `GET /api/v1/notifications/unread-count`, `PUT /api/v1/notifications/:id/read`, `PUT /api/v1/notifications/read-all`, `POST/DELETE /api/v1/notifications/fcm-token` (stub).
- **Reports:** `POST /api/v1/reports`; **Admin:** `GET /api/v1/reports/admin`, `GET /api/v1/reports/admin/:id`, `PUT /api/v1/reports/admin/:id/resolve` (stub).
- **Admin:** `GET /api/v1/admin/users`, `PUT /api/v1/admin/users/:id/block`, `PUT /api/v1/admin/users/:id/unblock`, `GET /api/v1/admin/listings/pending`, `PUT /api/v1/admin/listings/:id/approve`, `PUT /api/v1/admin/listings/:id/reject` (stub).
- **Chat:** `POST/GET /api/v1/chats`, `GET /api/v1/chats/:room_id/messages`, `POST /api/v1/chats/:room_id/messages`, `PUT /api/v1/chats/:room_id/read` (stub). **SignalR:** `/hubs/chat` — JoinRoom, LeaveRoom, SendMessage, TypingStart, TypingStop, MarkRead; events NewMessage, UserTyping, UserStopTyping, MessageRead, UserOnline, UserOffline.
- **Health:** `GET /health` (status, checks: postgres, redis, elasticsearch).
- **Metrics:** `GET /metrics` (Prometheus).
- **Rate limit:** Redis-backed, global per IP/user (config: RateLimit:MaxRequestsPerMinute).

Response format: `{ "success": true, "data": ... }` or `{ "success": false, "error": { "code", "message", "details" } }`.  
JSON uses snake_case. Pagination: `data.items` + `data.meta` (page, per_page, total, total_pages).

## Remaining (per plan)

- Notifications, Reports, Admin: stubbed responses; wire to real repositories/services for full behavior.
- Chat REST + SignalR: stubbed/create-or-get; wire to ChatRoom/Message repositories and add WS rate limit (30 msg/min/room) for production.
- Elasticsearch listing search and PostGIS nearby: TODO in ListingService.
- FluentValidation auto-validation filter for consistent VALIDATION_ERROR response shape.
