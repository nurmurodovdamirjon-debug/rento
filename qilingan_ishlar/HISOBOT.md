# RENTO — Qilingan Ishlar Hisoboti

> Oxirgi yangilangan: 2026-yil 15-fevral
> Holat: Sprint 6 tugallandi, Sprint 7 boshlandi
> Branch: `develop` (6 ta sprint merged)

---

## 📊 UMUMIY STATISTIKA

| Ko'rsatkich | Qiymat |
|-------------|--------|
| Jami sprintlar | 8 ta (6 ta tugallandi, 2 ta qoldi) |
| Jami commitlar | 12 ta |
| Jami fayllar o'zgargan | 199 ta |
| Jami qatorlar qo'shilgan | 31,833+ |
| Backend fayllar (Go + TS) | ~121 ta |
| Android fayllar (Kotlin) | 95 ta |
| DB migratsiyalar | 8 juft (up + down) |
| Testlar | 54 ta (chat-service) + auth-service testlari |

---

## 🏗️ ARXITEKTURA

```
rento/
├── services/
│   ├── auth-service/      — Node.js + TypeScript + Express (port 3001)
│   ├── chat-service/      — Node.js + TypeScript + Express + Socket.IO (port 3003)
│   └── core-api/          — Go + Gin + sqlx + zerolog (port 8080)
├── mobile/
│   └── android/           — Kotlin + Jetpack Compose + Hilt + Retrofit
├── gateway/               — Nginx reverse proxy config
├── migrations/            — PostgreSQL migratsiyalar (000001-000008)
├── docker-compose.yml     — PostgreSQL, Redis, MinIO, Elasticsearch
└── docs/                  — 25 ta hujjat (NDA, PRD, API, Deployment, ...)
```

---

## ✅ SPRINT 0: MUHIT TAYYORLASH

**Commit:** `chore(infra): Sprint 0 — muhit tayyorlash tugallandi`

- Git repo initializatsiya (main → develop flow)
- Docker Compose: PostgreSQL 15, Redis 7, MinIO, Elasticsearch 8
- Go project scaffolding (core-api)
- Node.js project scaffolding (auth-service, chat-service)
- Android project scaffolding (Jetpack Compose + Hilt)
- Nginx gateway konfiguratsiya
- .env, .editorconfig, Makefile

---

## ✅ SPRINT 1: AUTH SERVICE (Autentifikatsiya)

**Commit:** `feat(auth): implement SMS OTP authentication`

### Yaratilgan:
- **OTP autentifikatsiya**: telefon → SMS OTP → JWT (access + refresh)
- `POST /auth/send-otp` — OTP yuborish (rate limited)
- `POST /auth/verify-otp` — OTP tasdiqlash, JWT pair qaytarish
- `POST /auth/refresh` — Token rotation
- `POST /auth/logout` — Session o'chirish
- JWT: access (15 min) + refresh (7 kun) token pair
- Redis session management
- Rate limiting (otp: 3/min, verify: 5/min)
- Zod validation, error handling, logging
- Users jadval migratsiya (000001)

---

## ✅ SPRINT 2: USER PROFILE + ANDROID SCAFFOLDING

**Commit:** `feat(S2): User Profile API + Android project — 35/35 tasks`

### Backend:
- `GET /users/me` — o'z profilni olish
- `PUT /users/me` — profil tahrirlash
- `GET /users/:id` — boshqa foydalanuvchi profili
- Avatar upload (MinIO)
- Android project skeleton

### Android:
- Hilt DI setup
- Retrofit + OkHttp client (auth interceptor)
- Login → OTP → Home flow
- Splash, Onboarding, Profile, EditProfile ekranlar
- Bottom navigation bar (Home, Search, Create, Chat, Profile)
- Material3 tema (RentoPrimary, typography)

---

## ✅ SPRINT 3: LISTINGS CRUD (E'lonlar)

**Commit:** `feat(sprint-3): Listings CRUD + Media upload + Android UI`

### Database:
- `000002_create_listings` — 35+ ustunli listings jadval
- `000003_create_listing_images` — rasmlar jadvali

### Backend (Go):
- **Listing CRUD**: Create, Read, Update, Delete
- **Status management**: pending → active → rented/archived/expired
- **Rasm upload**: MinIO storage, thumbnail generatsiya
- **Filtrlash**: city, type, deal_type, rooms, price range, sort
- **Statistika**: views, favorites, contacts count
- **Kunlik limit**: max 10 ta e'lon

### Android:
- HomeScreen (e'lonlar ro'yxati, filtrlar)
- ListingDetailScreen (rasm galereyasi, xususiyatlar, joylashuv)
- CreateListingScreen (ko'p bosqichli forma)
- MyListingsScreen (status bilan filtrlash)
- ListingCard komponenti (Premium badge, rasm soni, statistika)

---

## ✅ SPRINT 4: SEARCH + FILTERS (Qidiruv)

**Commit:** `feat(sprint-4): search + filters — ES integration, PostGIS nearby, Android search/map UI`

### Backend:
- **Elasticsearch integratsiya**: full-text search, fuzzy matching
- **PostGIS**: yaqin atrofdagi e'lonlar (radius search, Haversine)
- **Redis cache**: search natijalari kesh
- **Filtrlash**: narx diapazoni, xonalar soni, maydon, mebel va boshqalar

### Android:
- SearchScreen (real-time qidiruv + FilterSheet)
- MapScreen (Google Maps + marker cluster)
- FilterSheet (BottomSheet: city, type, rooms, price, amenities)
- SearchBar komponenti

---

## ✅ SPRINT 5: CHAT (Xabar almashish)

**Commit:** `feat(S5): Chat - backend Node.js service + Android client (REST + WebSocket)`

### Database:
- `000004_create_chat_rooms` — chat xonalari
- `000005_create_messages` — xabarlar (text, image, voice, location, system)

### Backend (chat-service):
- **REST API**: chat yaratish, xabarlar tarixi, xabar yuborish
- **WebSocket (Socket.IO)**: real-time xabar, typing indicator, online status
- **54 ta test**: validator, service, controller, middleware, Socket.IO
- **Xabar turlari**: text, image, voice, location, system
- **O'qildi/O'qilmadi**: read receipts

### Android:
- ChatListScreen (xabarlar ro'yxati, unread badge)
- ChatScreen (real-time xabar, typing indicator)
- Socket.IO client (auto-reconnect, event handling)
- ChatListingPreview komponenti

---

## ✅ SPRINT 6: FAVORITES + NOTIFICATIONS (Sevimlilar va Bildirishnomalar)

**Commit:** `feat(S6): Favorites + Notifications — full stack implementation`

### Bug Fixes (3 ta):
1. **auth-service**: `AppError.prototype` → `new.target.prototype` (instanceof fix)
2. **Go response**: Paginated format → `{ data: { items: [...], meta: {...} } }` (Android moslashuvi)
3. **chat controller**: Javob formati to'g'rilandi

### Database:
- `000006_create_favorites` — sevimlilar (UNIQUE user_id + listing_id)
- `000007_create_notifications` — bildirishnomalar + FCM tokens
- `000008_create_saved_searches` — saqlangan qidiruvlar

### Backend (Go):
- **Favorites**: toggle, list, check, batch IDs (4 endpoint)
- **Notifications**: list, unread-count, read, read-all, FCM token CRUD (6 endpoint)
- **FCM push**: stub (Sprint 7 da Firebase Admin SDK qo'shiladi)

### Android:
- FavoritesScreen + FavoritesViewModel (LazyColumn, swipe-to-delete)
- NotificationsScreen + NotificationsViewModel (unread badge, mark all read)
- ListingCard ❤️ toggle (isFavorite + onToggleFavorite)
- HomeViewModel favorite integration (batch check)
- ProfileScreen → Favorites/Notifications navigatsiya
- RentoFirebaseMessagingService (FCM push qabul)
- DI: NetworkModule + AppModule yangilandi
- 7 ta UseCase (Toggle, GetFavorites, GetIds, GetNotifications, UnreadCount, MarkRead, FCM)

---

## 🔜 SPRINT 7: MODERATSIYA + BUG FIX (Hozirgi)

**Holat:** Boshlandi
**Branch:** `feature/RENTO-S7-moderation-bugfix`

### Rejalashtirilgan:
- Report module (shikoyat yuborish)
- Admin moderatsiya (approve/reject e'lonlar)
- Admin middleware (role='admin' tekshiruv)
- User bloklash (admin tomonidan)
- Blocked user login bloklash
- Performance optimization (N+1, Redis cache)
- Android UI polish (loading, empty, error states)
- Bug fix

---

## 🔜 SPRINT 8: DEPLOY + BETA (Oxirgi)

### Rejalashtirilgan:
- VPS setup (4vCPU, 8GB)
- Docker production build
- Nginx + SSL (Let's Encrypt)
- CI/CD pipeline
- Google Play Console beta
- Monitoring (Prometheus + Grafana)

---

## 🛠️ TEXNOLOGIYALAR VA STACK

| Qatlam | Texnologiya |
|--------|-------------|
| **Go Backend** | Go 1.21, Gin, sqlx, zerolog, google/uuid, go-redis, jwt-go |
| **Auth Service** | Node.js 18, TypeScript, Express, pg, ioredis, JWT, Zod v3 |
| **Chat Service** | Node.js 18, TypeScript, Express, Socket.IO, pg, ioredis, Zod v4 |
| **Android** | Kotlin, Jetpack Compose, Hilt, Retrofit, Material3, Coil, Socket.IO |
| **Database** | PostgreSQL 15 (UUID PK, JSONB, PostGIS) |
| **Cache** | Redis 7 (sessions, rate-limit, cache) |
| **Search** | Elasticsearch 8 (full-text search) |
| **Storage** | MinIO (S3-compatible, rasm va media) |
| **Gateway** | Nginx (reverse proxy, SSL termination) |
| **DevOps** | Docker Compose, Git (main → develop → feature branches) |

---

## 📁 API ENDPOINTS XULOSA

### Auth Service (port 3001)
| Method | Endpoint | Tavsif |
|--------|----------|--------|
| POST | /auth/send-otp | OTP yuborish |
| POST | /auth/verify-otp | OTP tasdiqlash |
| POST | /auth/refresh | Token yangilash |
| POST | /auth/logout | Chiqish |

### Core API (port 8080)
| Method | Endpoint | Tavsif |
|--------|----------|--------|
| GET | /api/v1/users/me | O'z profilim |
| PUT | /api/v1/users/me | Profil tahrirlash |
| GET | /api/v1/users/:id | Boshqa profil |
| GET | /api/v1/listings | E'lonlar ro'yxati |
| POST | /api/v1/listings | E'lon yaratish |
| GET | /api/v1/listings/:id | E'lon batafsil |
| PUT | /api/v1/listings/:id | E'lon tahrirlash |
| DELETE | /api/v1/listings/:id | E'lon o'chirish |
| PUT | /api/v1/listings/:id/status | Status o'zgartirish |
| GET | /api/v1/listings/:id/stats | Statistika |
| GET | /api/v1/listings/my | Mening e'lonlarim |
| GET | /api/v1/listings/search | Qidiruv |
| GET | /api/v1/listings/nearby | Yaqin atrofdagilar |
| POST | /api/v1/media/upload | Rasm yuklash |
| POST | /api/v1/favorites/:listing_id | Sevimli toggle |
| GET | /api/v1/favorites | Sevimlilar ro'yxati |
| GET | /api/v1/favorites/check/:listing_id | Sevimli tekshirish |
| GET | /api/v1/favorites/ids | Sevimli IDlar |
| GET | /api/v1/notifications | Bildirishnomalar |
| GET | /api/v1/notifications/unread-count | O'qilmaganlar soni |
| PUT | /api/v1/notifications/:id/read | O'qilgan belgilash |
| PUT | /api/v1/notifications/read-all | Barchasini o'qish |
| POST | /api/v1/notifications/fcm-token | FCM token saqlash |
| DELETE | /api/v1/notifications/fcm-token | FCM token o'chirish |

### Chat Service (port 3003)
| Method | Endpoint | Tavsif |
|--------|----------|--------|
| POST | /api/v1/chats | Chat yaratish |
| GET | /api/v1/chats | Chatlar ro'yxati |
| GET | /api/v1/chats/:roomId/messages | Xabarlar tarixi |
| POST | /api/v1/chats/:roomId/messages | Xabar yuborish |
| PUT | /api/v1/chats/:roomId/read | O'qildi belgilash |
| WS | /socket.io | Real-time xabar |

---

## 📱 ANDROID EKRANLAR

| Ekran | Fayl | Tavsif |
|-------|------|--------|
| Splash | SplashScreen.kt | Ilovaga kirish |
| Onboarding | OnboardingScreen.kt | 3 sahifali tanishtirish |
| Login | LoginScreen.kt | Telefon raqam kiritish |
| OTP | OtpScreen.kt | Tasdiqlash kodi |
| Home | HomeScreen.kt | E'lonlar ro'yxati + filtr |
| Detail | ListingDetailScreen.kt | E'lon batafsil (galeriya) |
| Create | CreateListingScreen.kt | Yangi e'lon yaratish |
| My Listings | MyListingsScreen.kt | Mening e'lonlarim |
| Search | SearchScreen.kt | Qidiruv + FilterSheet |
| Map | MapScreen.kt | Xarita (yaqinlar) |
| Chat List | ChatListScreen.kt | Xabarlar ro'yxati |
| Chat | ChatScreen.kt | Suhbat ekrani |
| Profile | ProfileScreen.kt | Profil |
| Edit Profile | EditProfileScreen.kt | Profil tahrirlash |
| Favorites | FavoritesScreen.kt | Sevimlilar |
| Notifications | NotificationsScreen.kt | Bildirishnomalar |

---

*Bu hujjat avtomatik yaratilgan. Oxirgi yangilash: 2026-02-15*
