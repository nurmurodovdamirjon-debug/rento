.PHONY: up down logs ps migrate-up migrate-down test lint build seed clean restart

# ===== DOCKER =====
up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

ps:
	docker compose ps

restart:
	docker compose down && docker compose up -d

clean:
	docker compose down -v

# ===== INFRASTRUCTURE ONLY (no app services) =====
infra-up:
	docker compose up -d postgres redis elasticsearch minio rabbitmq

infra-down:
	docker compose down

# ===== BUILD =====
build:
	docker compose build

rebuild:
	docker compose build --no-cache

# ===== DATABASE =====
migrate-up:
	@echo "Migration up — Sprint 1 da implement qilinadi"

migrate-down:
	@echo "Migration down — Sprint 1 da implement qilinadi"

seed:
	@echo "Seed data — Sprint 1 da implement qilinadi"

# ===== TEST =====
test:
	@echo "Running all tests..."
	cd services/core-api && go test ./...
	cd services/auth-service && npm test
	cd services/chat-service && npm test

# ===== LINT =====
lint:
	@echo "Running linters..."
	cd services/core-api && golangci-lint run
	cd services/auth-service && npm run lint
	cd services/chat-service && npm run lint

# ===== HEALTH CHECK =====
health:
	@echo "=== Auth Service ==="
	@curl -s http://localhost:3001/health | python -m json.tool 2>/dev/null || echo "Auth service not running"
	@echo "\n=== Core API ==="
	@curl -s http://localhost:3002/health | python -m json.tool 2>/dev/null || echo "Core API not running"
	@echo "\n=== Chat Service ==="
	@curl -s http://localhost:3003/health | python -m json.tool 2>/dev/null || echo "Chat service not running"

# ===== DB SHELL =====
db-shell:
	docker compose exec postgres psql -U rento_user -d rento

redis-shell:
	docker compose exec redis redis-cli
