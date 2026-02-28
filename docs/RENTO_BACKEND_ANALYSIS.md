# Rento backend — to'liq analiz

Bu hujjat Rento loyihasining hozirgi backend arxitekturasi, API kontraktlari va ma'lumotlar bazasi sxemasini to'liq tavsiflaydi. C# ga migratsiya yoki boshqa tilga qayta yozish uchun asos sifatida ishlatiladi.

---

## 1. Umumiy arxitektura

### 1.1 Servislar

| Servis | Til | Port | Vazifa |
|--------|-----|------|--------|
| **core-api** | Go (Gin) | 3002 | Listing, user, favorite, notification, report, media, admin |
| **auth-service** | Node.js (Express) | 3001 | OTP, JWT, refresh token, logout |
| **chat-service** | Node.js (Express + Socket.IO) | 3003 | Chat REST + WebSocket (/ws/chat) |

### 1.2 Gateway

- **Nginx** bitta kirish nuqtasi:
  - `/api/v1/auth/*` → auth-service:3001
  - `/api/v1/*` (auth dan boshqa) → core-api:3002
  - `/ws/*` → chat-service:3003 (WebSocket upgrade)

### 1.3 Infrastruktura

- **PostgreSQL** — barcha servislar ulangan (bitta DB: `rento`)
- **Redis** — auth session, OTP, rate limit, chat online status
- **Elasticsearch** — listing qidiruv (core-api)
- **MinIO** — media (listing rasmlar, avatarlar)

---

## 2. API kontraktlari

### 2.1 Umumiy javob formati

**Muvaffaqiyat (200/201):**
```json
{
  "success": true,
  "data": { ... }
}
```

**Pagination (200):**
```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    "meta": {
      "page": 1,
      "per_page": 20,
      "total": 100,
      "total_pages": 5
    }
  }
}
```

**Xato (4xx/5xx):**
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Xabar matni",
    "details": { ... }
  }
}
```

**Auth header:** `Authorization: Bearer <access_token>`

**JWT claims (access):** `sub` (user id), `role`, `phone`, `iss: "rento.uz"`

---

### 2.2 Auth (auth-service) — `/api/v1/auth`

| Method | Path | Auth | Tavsif |
|--------|------|------|--------|
| POST | /send-otp | Yo'q | SMS OTP yuborish. Body: `{ "phone": "+998901234567" }` |
| POST | /verify-otp | Yo'q | OTP tasdiqlash. Body: `{ "phone": "+998...", "otp": "123456" }` |
| POST | /refresh-token | Yo'q | Token yangilash. Body: `{ "refresh_token": "..." }` |
| POST | /logout | Ha | Chiqish. Session (Redis) o'chiriladi |

**send-otp request:** `{ "phone": "+998XXXXXXXXX" }` — 13 belgi, regex: `^\+998\d{9}$`

**send-otp response:** `{ "phone", "expires_in": 300, "retry_after": 60, "attempts_remaining" }`

**verify-otp response:**  
`{ "access_token", "refresh_token", "token_type": "Bearer", "expires_in": 900, "user": { "id", "phone", "full_name", "role", "is_new_user" } }`

**refresh-token response:** `{ "access_token", "refresh_token", "token_type", "expires_in" }`

---

### 2.3 Core API (core-api) — `/api/v1`

#### User — `/users`

| Method | Path | Auth | Tavsif |
|--------|------|------|--------|
| GET | /users/me | Ha | O'z profili |
| PUT | /users/me | Ha | Profil yangilash. Body: `{ "full_name?", "email?", "language?" }` |
| GET | /users/:id | Ha | Ochiq profil (boshqa user) |

**Profil yangilash:** `full_name` (2–100), `email` (email), `language` (uz|ru|en). Kamida bitta maydon.

#### Listing — `/listings`

| Method | Path | Auth | Tavsif |
|--------|------|------|--------|
| GET | /listings | Ixtiyoriy | Ro'yxat. Query: city, district, type, deal_type, rooms_min/max, price_min/max, currency, has_furniture, has_parking, allows_pets, sort, page, per_page |
| GET | /listings/search | Ixtiyoriy | ES qidiruv. Query: q, city, type, deal_type, sort (relevance\|newest\|price_asc\|price_desc), page, per_page |
| GET | /listings/nearby | Ixtiyoriy | Yaqin atrof. Query: lat, lng, radius_km, type, deal_type, page, per_page |
| GET | /listings/:id | Ixtiyoriy | Bitta e'lon |
| POST | /listings | Ha | E'lon yaratish |
| PUT | /listings/:id | Ha | E'lon tahrirlash |
| DELETE | /listings/:id | Ha | E'lon o'chirish |
| PUT | /listings/:id/status | Ha | Status: active, rented, archived |
| GET | /listings/:id/stats | Ha | Ko'rish/sevimli/kontakt statistikasi (egasi) |
| GET | /listings/my | Ha | Mening e'lonlarim. Query: status?, page, per_page |

**Create listing request:** type (apartment|house|office|shop|warehouse), deal_type? (rent|daily), city, district?, address?, landmark?, latitude?, longitude?, rooms?, floor?, total_floors?, area_sqm?, price, currency (UZS|USD), price_negotiable?, has_furniture?, has_appliances?, has_internet?, has_parking?, has_conditioner?, allows_pets?, allows_children?, utilities_included?, deposit_amount?, title (5–200), description? (20–5000).

**Update status request:** `{ "status": "active" | "rented" | "archived" }`

**Kunlik limit:** 10 ta e'lon/kun (create).

#### Media — `/media`

| Method | Path | Auth | Tavsif |
|--------|------|------|--------|
| POST | /media/upload | Ha | Multipart: file, bucket (listings|avatars). Max 5MB, JPEG/PNG/WebP |

#### Favorites — `/favorites`

| Method | Path | Auth | Tavsif |
|--------|------|------|--------|
| POST | /favorites/:listing_id | Ha | Toggle (qo'shish/olib tashlash) |
| GET | /favorites | Ha | Ro'yxat. page, per_page |
| GET | /favorites/check/:listing_id | Ha | Tekshirish. Response: `{ "is_favorite": true }` |
| GET | /favorites/ids | Ha | Barcha sevimli listing_id lar. Response: `{ "listing_ids": ["uuid", ...] }` |

#### Notifications — `/notifications`

| Method | Path | Auth | Tavsif |
|--------|------|------|--------|
| GET | /notifications | Ha | Ro'yxat. page, per_page |
| GET | /notifications/unread-count | Ha | `{ "unread_count": 0 }` |
| PUT | /notifications/:id/read | Ha | O'qildi |
| PUT | /notifications/read-all | Ha | Barchasini o'qildi. `{ "marked_count": 5 }` |
| POST | /notifications/fcm-token | Ha | Body: `{ "token", "device_type?" }` |
| DELETE | /notifications/fcm-token | Ha | Body: `{ "token" }` |

#### Reports — `/reports`

| Method | Path | Auth | Tavsif |
|--------|------|------|--------|
| POST | /reports | Ha | Shikoyat. Body: target_type (listing|user|message), target_id (uuid), reason (spam|fraud|inappropriate|duplicate|wrong_info|offensive|illegal|other), description? |
| GET | /reports/admin | Admin | Ro'yxat. Query: status?, page, per_page |
| GET | /reports/admin/:id | Admin | Bitta |
| PUT | /reports/admin/:id/resolve | Admin | Body: status (resolved|dismissed), admin_note? |

#### Admin — core-api

| Method | Path | Auth | Tavsif |
|--------|------|------|--------|
| GET | /admin/users | Admin | Ro'yxat. page, per_page |
| PUT | /admin/users/:id/block | Admin | Bloklash |
| PUT | /admin/users/:id/unblock | Admin | Blokdan chiqarish |
| GET | /admin/listings/pending | Admin | Moderatsiya: pending e'lonlar |
| PUT | /admin/listings/:id/approve | Admin | Tasdiqlash |
| PUT | /admin/listings/:id/reject | Admin | Rad etish |

**Admin roli:** JWT da `role === "admin"`.

---

### 2.4 Chat (chat-service) — `/api/v1/chats`

| Method | Path | Auth | Tavsif |
|--------|------|------|--------|
| POST | /chats | Ha | Yangi chat. Body: `{ "listing_id": "uuid", "initial_message": "..." }` |
| GET | /chats | Ha | Chat ro'yxati. page, per_page |
| GET | /chats/:room_id/messages | Ha | Xabarlar. page, per_page |
| POST | /chats/:room_id/messages | Ha | Xabar yuborish (REST). Body: content?, message_type?, media_url?, metadata? |
| PUT | /chats/:room_id/read | Ha | O'qildi (204) |

**createChat body:** listing_id (uuid), initial_message (1–2000 belgi).

**sendMessage body:** content? (text uchun majburiy), message_type (text|image|location|contact), media_url?, metadata?.

---

### 2.5 WebSocket (chat-service) — path: `/ws/chat`

**Ulanish:** Socket.IO client, path `/ws/chat`. Auth: query yoki handshake da token (auth-service JWT).

**Client → Server events:**

| Event | Payload | Tavsif |
|-------|---------|--------|
| join_room | `{ room_id }` | Xonaga kirish |
| leave_room | `{ room_id }` | Chiqish |
| send_message | `{ room_id, content?, type?, media_url?, metadata? }` | Xabar. type: text\|image\|location\|contact |
| typing_start | `{ room_id }` | Yozayotganini bildirish |
| typing_stop | `{ room_id }` | To'xtatish |
| mark_read | `{ room_id, message_id }` | O'qildi |
| ping | — | pong + online TTL yangilash |

**Server → Client events:**

| Event | Payload |
|-------|---------|
| new_message | id, room_id, sender_id, content, type, media_url, metadata, created_at |
| user_typing | room_id, user_id |
| user_stop_typing | room_id, user_id |
| message_read | room_id, reader_id, last_read_id |
| user_online | user_id |
| user_offline | user_id |
| pong | {} |
| error | code, message |

**Rate limit:** 30 xabar/min/xona (Redis).

---

## 3. Ma'lumotlar bazasi (PostgreSQL)

### 3.1 Jadvalar

- **users** — id, phone, phone_verified, full_name, email, avatar_url, role (tenant|landlord|both|admin), id_verified, id_document_url, id_verified_at, rating_avg, rating_count, subscription (free|pro), sub_expires_at, language (uz|ru|en), last_seen_at, created_at, updated_at, is_active, is_blocked.
- **listings** — id, user_id, type, deal_type, city, district, address, landmark, latitude, longitude, rooms, floor, total_floors, area_sqm, price, currency, price_negotiable, has_furniture, has_appliances, has_internet, has_parking, has_conditioner, allows_pets, allows_children, utilities_included, deposit_amount, status (pending|active|rejected|rented|archived), rejection_reason, is_premium, premium_until, views_count, favorites_count, contacts_count, title, description, published_at, expires_at, created_at, updated_at.
- **listing_images** — id, listing_id, url, thumbnail_url, sort_order, is_main, created_at.
- **chat_rooms** — id, listing_id, tenant_id, landlord_id, last_message_at, is_active, created_at. UNIQUE(listing_id, tenant_id, landlord_id).
- **messages** — id, room_id, sender_id, content, message_type (text|image|location|contact), media_url, metadata (JSONB), is_read, read_at, created_at.
- **favorites** — id, user_id, listing_id, created_at. UNIQUE(user_id, listing_id).
- **notifications** — id, user_id, type, title, body, ref_type, ref_id, is_read, read_at, push_sent, push_sent_at, metadata, created_at.
- **fcm_tokens** — id, user_id, token, device_type, is_active, created_at, updated_at. UNIQUE(user_id, token).
- **reports** — id, reporter_id, target_type (listing|user|message), target_id, reason, description, status (pending|reviewing|resolved|dismissed), admin_note, resolved_by, resolved_at, created_at, updated_at. UNIQUE(reporter_id, target_type, target_id).

### 3.2 Elasticsearch

- Indeks: listinglar uchun (core-api da). Mapping: title, description, city, type, deal_type, price, va hokazo. Full-text qidiruv.

### 3.3 Redis

- **auth-service:** `auth:otp:{phone}` (OTP, TTL 300s), `auth:otp_attempts:{phone}` (soatiga 3 SMS), `auth:session:{userId}` (refresh token, 7 kun).
- **core-api:** rate limit (RATE_LIMIT_MAX_REQUESTS), listing cache (ixtiyoriy).
- **chat-service:** online user TTL, `ws:ratelimit:{room_id}:{userId}` (30/min).

---

## 4. Konfiguratsiya (environment)

### 4.1 Umumiy

- APP_ENV, APP_VERSION, CORS_ALLOWED_ORIGINS
- DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, DB_SSL_MODE, DB_MAX_CONNECTIONS
- REDIS_HOST, REDIS_PORT, REDIS_PASSWORD
- JWT_ACCESS_SECRET, JWT_REFRESH_SECRET, JWT_ACCESS_EXPIRY (15m), JWT_REFRESH_EXPIRY (7d)

### 4.2 core-api

- APP_PORT=3002
- ES_URL (Elasticsearch)
- MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_USE_SSL
- RATE_LIMIT_MAX_REQUESTS (60)

### 4.3 auth-service

- AUTH_SERVICE_PORT=3001
- SMS: SMS_PROVIDER (eskiz), ESKIZ_EMAIL, ESKIZ_PASSWORD, ESKIZ_BASE_URL
- RATE_LIMIT_SMS_MAX (3/soat), RATE_LIMIT_SMS_WINDOW (3600)
- OTP: 6 raqam, 300s TTL

### 4.4 chat-service

- CHAT_SERVICE_PORT=3003
- WS_RATE_LIMIT_MAX (30), WS_RATE_LIMIT_WINDOW (60000 ms)

---

## 5. Xato kodlari (API)

AUTH_REQUIRED, AUTH_TOKEN_INVALID, AUTH_TOKEN_EXPIRED, USER_NOT_FOUND, USER_BLOCKED, LISTING_NOT_FOUND, LISTING_NOT_OWNER, INVALID_ID, VALIDATION_ERROR, NOT_FOUND, RATE_LIMIT_EXCEEDED, INTERNAL_ERROR, ADMIN_REQUIRED, ALREADY_REPORTED, SELF_REPORT, REPORT_NOT_FOUND, INVALID_STATUS_TRANSITION, FILE_REQUIRED, MEDIA_TOO_LARGE, MEDIA_INVALID_TYPE, INVALID_BUCKET.

---

## 6. Mobil ilova

- Android (Kotlin). API base URL — gateway (nginx). Barcha `/api/v1/*` va `/ws/chat` shu kontraktga tayanadi. C# da API va WebSocket kontraktlari o'zgartirilmasa, mobil ilova o'zgartirilmaydi.
