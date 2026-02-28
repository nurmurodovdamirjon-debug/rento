# Rento backend — C# ga to'liq migratsiya prompti

Bu prompt Rento loyihasining backend qismini Go + Node.js dan **bitta ASP.NET Core (C#) ilovasiga** to'liq ko'chirish uchun ishlatiladi. Barcha API kontraktlar va biznes-logika hujjatlashtirilgan analizga asosan saqlanadi; mobil ilova o'zgartirilmaydi.

---

## Vazifa

Rento backendini **C# (.NET 8, ASP.NET Core)** da qayta yozing. Hozirgi tizimda uchta servis bor: **core-api** (Go), **auth-service** (Node.js), **chat-service** (Node.js). Barcha funksiyani **bitta ASP.NET Core Web API** da birlashtiring (monolit). API endpointlar, request/response formatlari va WebSocket eventlar **analiz hujjatidagi** (RENTO_BACKEND_ANALYSIS.md) kontraktlarga **100% mos** bo'lsin — mobil Android ilova o'zgartirilmasin.

---

## Texnologiyalar

- **.NET 8**, **ASP.NET Core Web API**
- **Entity Framework Core** — PostgreSQL (mavjud migratsiyalar/sxema saqlanadi)
- **Redis** — StackExchange.Redis (session, OTP, rate limit, chat online)
- **Elasticsearch** — NEST yoki Elastic.Clients.Elasticsearch (listing qidiruv)
- **MinIO / S3** — fayl saqlash (listing rasmlar, avatarlar). MinIO .NET client yoki AWS SDK S3-compatible endpoint uchun
- **JWT** — access + refresh token, issuer `rento.uz`, claimlar: sub, role, phone
- **SignalR** — real-time chat (Socket.IO o'rniga). **Muhim:** mobil ilova Socket.IO client ishlatadi; agar bitta backendda SignalR qilsangiz, mobil tomonda **SignalR client** ga o'tkazish yoki **Socket.IO protocol** ni C# da qo'llab-quvvatlash kerak. Agar Socket.IO protocol saqlansin desangiz: C# da Socket.IO server (masalan, Quobject.SocketIOClientNative yoki custom WebSocket handler) yoki alohida SignalR endpoint + mobil da yangi SDK. Bu qaror loyiha qarori; prompt da **real-time** uchun SignalR tavsiya etiladi, lekin **event nomlari va payload** analizdagi WebSocket bo'limiga mos bo'lsin (new_message, user_typing, mark_read va hokazo).
- **Serilog** — logging
- **Health checks** — /health (PostgreSQL, Redis, Elasticsearch, MinIO)
- **Prometheus metrics** — /metrics (AspNetCore.Diagnostics.HealthChecks yoki Prometheus.Net)

---

## Loyiha tuzilishi

- **Rento.Api** — ASP.NET Core Web API (bitta entry point). Controllerlar: AuthController, UsersController, ListingsController, MediaController, FavoritesController, NotificationsController, ReportsController, ChatsController. Middleware: JWT auth, admin role, CORS, rate limit (Redis), request logging, global exception handler.
- **Rento.Core** — domain modellar, DTO (request/response), xato kodlari, validatorlar (FluentValidation yoki DataAnnotations).
- **Rento.Infrastructure** — EF Core DbContext, repositorylar, Elasticsearch client, Redis, MinIO/S3, SMS (Eskiz) client, JWT generation/validation.
- **Rento.Application** (ixtiyoriy) — use case / service layer: AuthService, UserService, ListingService, FavoriteService, NotificationService, ReportService, ChatService, MessageService, MediaService.

Alternativ: **Vertical Slice** — har bir feature uchun bitta papka (Auth, Users, Listings, …) ichida Controller, Service, Repository, DTO. Tanlashingizga qarab layering yoki slice.

---

## API kontraktlar — majburiy qoidalar

1. **Base path:** `/api/v1`. Auth: `/api/v1/auth/*`. User: `/api/v1/users/*`. Listing: `/api/v1/listings/*`. Media: `/api/v1/media/*`. Favorites: `/api/v1/favorites/*`. Notifications: `/api/v1/notifications/*`. Reports: `/api/v1/reports/*`. Admin: `/api/v1/admin/users/*`, `/api/v1/admin/listings/*`, `/api/v1/reports/admin/*`. Chat REST: `/api/v1/chats/*`.
2. **Javob formati:** Muvaffaqiyat: `{ "success": true, "data": ... }`. Pagination: `data` ichida `items` + `meta` (page, per_page, total, total_pages). Xato: `{ "success": false, "error": { "code": "...", "message": "...", "details": ... } }`. HTTP status: 200, 201, 204, 400, 401, 403, 404, 409, 429, 500.
3. **Auth header:** `Authorization: Bearer <access_token>`. JWT claims: sub (user id), role, phone, iss = "rento.uz".
4. **Admin:** Faqat `role == "admin"` bo'lgan token uchun admin endpointlar ochiq.

Barcha endpointlar, query paramlar, body strukturalari va response maydonlari **RENTO_BACKEND_ANALYSIS.md** da berilgan jadval va misollarga mos bo'lsin.

---

## Auth moduli

- **POST /api/v1/auth/send-otp** — Body: `{ "phone": "+998XXXXXXXXX" }`. Validatsiya: 13 belgi, regex `^\+998\d{9}$`. Redis: OTP saqlash (TTL 300s), soatiga 3 ta SMS limit (Redis counter). SMS: Eskiz API (config: ESKIZ_EMAIL, ESKIZ_PASSWORD, ESKIZ_BASE_URL). Response: `{ "phone", "expires_in", "retry_after", "attempts_remaining" }`.
- **POST /api/v1/auth/verify-otp** — Body: `{ "phone", "otp": "123456" }`. OTP tekshirish, Redis dan o'chirish. User topish yoki yaratish (users jadvali). Bloklangan tekshirish (is_blocked). JWT pair (access + refresh). Refresh token Redis ga: key `auth:session:{userId}`, TTL 7 kun. Response: access_token, refresh_token, token_type: "Bearer", expires_in, user: { id, phone, full_name, role, is_new_user }.
- **POST /api/v1/auth/refresh-token** — Body: `{ "refresh_token": "..." }`. Refresh token verify (secret va issuer), Redis dan session tekshirish, yangi token pair, Redis session yangilash. Response: access_token, refresh_token, token_type, expires_in.
- **POST /api/v1/auth/logout** — Auth kerak. Redis dan `auth:session:{userId}` o'chirish. 200 yoki 204.

JWT: Access 15m, Refresh 7d. Signing: HMAC (JWT_ACCESS_SECRET, JWT_REFRESH_SECRET). Refresh payload: sub, role, phone, iss, jti, type: "refresh".

---

## User moduli

- **GET /api/v1/users/me** — JWT dan userId. Profil (to'liq). 401/403/404, USER_NOT_FOUND, USER_BLOCKED.
- **PUT /api/v1/users/me** — Body: full_name?, email?, language? (uz|ru|en). Kamida bitta maydon. 400 NO_FIELDS agar bo'sh.
- **GET /api/v1/users/:id** — Ochiq profil (boshqa user). :id != "me". UUID validatsiya.

---

## Listing moduli

- **GET /api/v1/listings** — Query: city, district, type, deal_type, rooms_min, rooms_max, price_min, price_max, currency, has_furniture, has_parking, allows_pets, sort, page, per_page. Paginated. Status active (yoki kerakli filter).
- **GET /api/v1/listings/search** — Elasticsearch. Query: q, city, type, deal_type, sort (relevance|newest|price_asc|price_desc), page, per_page.
- **GET /api/v1/listings/nearby** — PostGIS/geography. lat, lng, radius_km, type, deal_type, page, per_page. distance_meters in response.
- **GET /api/v1/listings/:id** — Bitta e'lon. :id "my" bo'lsa → GET /listings/my.
- **GET /api/v1/listings/my** — Auth. status?, page, per_page. Faqat o'z e'lonlari.
- **POST /api/v1/listings** — Auth. Body: CreateListingRequest (type, deal_type?, city, district?, address?, landmark?, latitude?, longitude?, rooms?, floor?, total_floors?, area_sqm?, price, currency, price_negotiable?, has_furniture?, has_appliances?, has_internet?, has_parking?, has_conditioner?, allows_pets?, allows_children?, utilities_included?, deposit_amount?, title, description?). Kunlik limit 10 ta (Redis yoki DB). Status pending.
- **PUT /api/v1/listings/:id** — Auth, egasi. UpdateListingRequest (partial). Status transition qoidalari.
- **DELETE /api/v1/listings/:id** — Auth, egasi.
- **PUT /api/v1/listings/:id/status** — Auth, egasi. Body: { "status": "active"|"rented"|"archived" }. Invalid transition → 400.
- **GET /api/v1/listings/:id/stats** — Auth, egasi. views, favorites, contacts.

Listing type: apartment|house|office|shop|warehouse. DealType: rent|daily. Currency: UZS|USD. Status: pending|active|rejected|rented|archived.

---

## Media moduli

- **POST /api/v1/media/upload** — Multipart: file, bucket (listings|avatars). Max 5MB. MIME: JPEG, PNG, WebP. MinIO/S3: bucket mavjudligi, object key (masalan user_id/filename). Response: { id, url, thumbnail_url?, sort_order?, is_main? } — analizdagi media response ga moslashtiring.

---

## Favorites moduli

- **POST /api/v1/favorites/:listing_id** — Toggle. Response: { is_favorite: true/false } yoki current state.
- **GET /api/v1/favorites** — page, per_page. Paginated (listing items).
- **GET /api/v1/favorites/check/:listing_id** — { "is_favorite": true/false }.
- **GET /api/v1/favorites/ids** — { "listing_ids": [ "uuid", ... ] }.

---

## Notifications moduli

- **GET /api/v1/notifications** — page, per_page. Paginated.
- **GET /api/v1/notifications/unread-count** — { "unread_count": 0 }.
- **PUT /api/v1/notifications/:id/read** — 204.
- **PUT /api/v1/notifications/read-all** — { "marked_count": N }.
- **POST /api/v1/notifications/fcm-token** — Body: token, device_type?.
- **DELETE /api/v1/notifications/fcm-token** — Body: { "token" }. 204.

---

## Reports moduli

- **POST /api/v1/reports** — Auth. target_type (listing|user|message), target_id (uuid), reason (spam|fraud|inappropriate|duplicate|wrong_info|offensive|illegal|other), description?. Bir user bir target ga bitta report. ALREADY_REPORTED, SELF_REPORT.
- **GET /api/v1/reports/admin** — Admin. status?, page, per_page. Paginated.
- **GET /api/v1/reports/admin/:id** — Admin.
- **PUT /api/v1/reports/admin/:id/resolve** — Admin. Body: status (resolved|dismissed), admin_note?.

---

## Admin moduli

- **GET /api/v1/admin/users** — page, per_page. Paginated.
- **PUT /api/v1/admin/users/:id/block** — is_blocked = true.
- **PUT /api/v1/admin/users/:id/unblock** — is_blocked = false.
- **GET /api/v1/admin/listings/pending** — status = pending. Paginated.
- **PUT /api/v1/admin/listings/:id/approve** — status → active.
- **PUT /api/v1/admin/listings/:id/reject** — status → rejected, rejection_reason optional.

---

## Chat moduli (REST)

- **POST /api/v1/chats** — Body: listing_id (uuid), initial_message (1–2000). createOrGetRoom (tenant = current user, landlord = listing owner). Yangi bo'lsa 201, mavjud bo'lsa 200. Response: room_id, listing { id, title, image_url }, other_user, created_at.
- **GET /api/v1/chats** — page, per_page. Ro'yxat: room_id, listing, other_user, last_message, unread_count va hokazo (analizdagi chat list formatiga mos).
- **GET /api/v1/chats/:room_id/messages** — Kirish huquqi (room a'zosi). page, per_page. Paginated messages.
- **POST /api/v1/chats/:room_id/messages** — Body: content?, message_type?, media_url?, metadata?. message_type: text|image|location|contact. Text uchun content majburiy. 201, data = message object.
- **PUT /api/v1/chats/:room_id/read** — markAsRead. 204.

---

## Real-time chat (WebSocket)

**Variant A — SignalR:** Hub path masalan `/hubs/chat`. Event nomlari va payload analizdagi Socket.IO eventlariga moslashtirilsin: JoinRoom(room_id), LeaveRoom(room_id), SendMessage(room_id, content, type, media_url, metadata), TypingStart(room_id), TypingStop(room_id), MarkRead(room_id, message_id). Server → client: NewMessage, UserTyping, UserStopTyping, MessageRead, UserOnline, UserOffline. Ping/Pong. Rate limit: 30 xabar/min/xona (Redis). Auth: QueryString yoki header dan token. Mobil ilova **SignalR client** (Microsoft.AspNetCore.SignalR.Client) ga o'tkaziladi.

**Variant B — Socket.IO protocol saqlash:** C# da Socket.IO server (third-party lib) yoki custom WebSocket handler orqali analizdagi event nomlari va payload larni aynan saqlang; mobil ilova o'zgartirilmaydi.

Qaysi variant tanlansa ham, **event nomlari va payload** RENTO_BACKEND_ANALYSIS.md dagi "WebSocket (chat-service)" jadvaliga mos bo'lsin.

---

## Ma'lumotlar bazasi

- **PostgreSQL:** Mavjud migratsiyalar (000001–000009) ishlatiladi. EF Core da DbSet: Users, Listings, ListingImages, ChatRooms, Messages, Favorites, Notifications, FcmTokens, Reports. Migrations papkasi yangi loyiha uchun mavjud SQL fayllarni EF migration sifatida import qilish yoki schema ni EF orqali yaratish.
- **Redis:** Key patternlar: auth:otp:{phone}, auth:otp_attempts:{phone}, auth:session:{userId}, rate limit keylar, ws:ratelimit:{room_id}:{userId}, chat online (user_online TTL).
- **Elasticsearch:** Listing indeks, mapping analizdagi core-api search moduliga mos. Index create/update on listing create/update/delete.

---

## Konfiguratsiya

appsettings.json / environment: APP_ENV, APP_VERSION, CORS_ALLOWED_ORIGINS. ConnectionStrings: DefaultConnection (PostgreSQL), Redis. JWT: AccessSecret, RefreshSecret, AccessExpiry, RefreshExpiry. MinIO/S3: Endpoint, AccessKey, SecretKey, UseSSL. Elasticsearch: Url. Eskiz: Email, Password, BaseUrl. RateLimit: MaxRequests (60), SmsMaxPerHour (3), SmsWindowSeconds (3600), WsRateLimitMax (30), WsRateLimitWindowMs (60000). OTP: Length (6), ExpirySeconds (300).

---

## Xavfsizlik va middleware

- CORS: Sozlanadigan originlar.
- Rate limit: Redis, global (masalan 60/min) va SMS/OTP limitlari.
- JWT: Bearer, issuer "rento.uz", validate lifetime. Claimlar: sub → userId, role, phone.
- Admin: [Authorize(Roles = "admin")] yoki custom filter.
- Global exception handler: AppError → code, message, details; 500 da INTERNAL_ERROR, message "Ichki server xatosi".
- Validation: FluentValidation yoki DataAnnotations, xato formati { "success": false, "error": { "code": "VALIDATION_ERROR", "message", "details" } }.

---

## Health va monitoring

- **GET /health** — status (ok/degraded), service, version, checks: postgres, redis, elasticsearch (yoki disabled), minio. 503 degraded.
- **GET /metrics** — Prometheus format.

---

## Qisqa tekshiruv ro'yxati

- [ ] Barcha endpoint path va method analizga mos.
- [ ] Request/response JSON formati (success, data, error, meta) va maydon nomlari (snake_case) analizga mos.
- [ ] JWT issuer "rento.uz", claimlar sub, role, phone.
- [ ] Auth, admin, rate limit, CORS ishlaydi.
- [ ] OTP, refresh token, logout Redis bilan.
- [ ] Listing CRUD, filter, search (ES), nearby (geo).
- [ ] Media upload (bucket, size, type).
- [ ] Favorites, notifications, reports, admin endpoints.
- [ ] Chat REST va WebSocket (event nomlari va payload).
- [ ] Health va metrics.
- [ ] Mobil ilova o'zgartirilmasdan ishlaydi (URL va kontraktlar bir xil).

---

**Hujjat:** Batafsil API va DB uchun `docs/RENTO_BACKEND_ANALYSIS.md` ga qarang.
