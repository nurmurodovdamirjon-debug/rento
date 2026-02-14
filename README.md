# Rento — Broker-free Rental Platform

O'zbekiston uchun vositachisiz ijara platformasi.

## Tech Stack

| Service | Technology | Port |
|---------|-----------|------|
| Gateway | Nginx | :80 |
| Auth Service | Node.js + Express + TypeScript | :3001 |
| Core API | Go + Gin | :3002 |
| Chat Service | Node.js + Socket.io | :3003 |
| Database | PostgreSQL 16 + PostGIS 3.4 | :5432 |
| Cache | Redis 7 | :6379 |
| Search | Elasticsearch 8.12.0 | :9200 |
| Storage | MinIO (S3) | :9000/:9001 |
| Queue | RabbitMQ 3 | :5672/:15672 |

## Quick Start

```bash
# 1. Environment sozlash
cp .env.example .env

# 2. Infra servislarni ishga tushirish
make infra-up

# 3. Barcha servislarni ishga tushirish
make up

# 4. Health check
make health
```

## Project Structure

```
rento/
├── services/
│   ├── core-api/       # Go (Gin) — Asosiy API
│   ├── auth-service/   # Node.js — Autentifikatsiya
│   └── chat-service/   # Node.js — Real-time chat
├── gateway/            # Nginx reverse proxy
├── scripts/            # DB init, seed, backup
├── mobile/             # Android (Kotlin + Jetpack Compose)
└── docs/api/           # Swagger/OpenAPI
```

## License

Private — All rights reserved.
