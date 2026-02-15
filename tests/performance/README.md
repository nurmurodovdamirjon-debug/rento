# RENTO Performance Tests (k6)

## O'rnatish

```bash
# macOS
brew install k6

# Windows
choco install k6

# Docker
docker pull grafana/k6
```

## Ishga tushirish

### Smoke Test (30 soniya, 1 VU)
```bash
k6 run --tag test_type=smoke tests/performance/k6-load-test.js
```

### To'liq test (barcha stsenariylar)
```bash
k6 run tests/performance/k6-load-test.js
```

### Maxsus sozlamalar bilan
```bash
k6 run \
  -e BASE_URL=https://staging.rento.uz \
  -e AUTH_URL=https://staging.rento.uz \
  -e API_URL=https://staging.rento.uz \
  -e CHAT_URL=https://staging.rento.uz \
  tests/performance/k6-load-test.js
```

### Docker orqali
```bash
docker run --rm -i \
  -e BASE_URL=http://host.docker.internal:80 \
  -v $(pwd)/tests/performance:/scripts \
  grafana/k6 run /scripts/k6-load-test.js
```

## Stsenariylar

| Stsenariy | VUs | Davomiyligi | Maqsad |
|-----------|-----|-------------|--------|
| Smoke | 1 | 30s | Bazaviy ishlashni tekshirish |
| Average | 0→20→0 | 5min | Kunlik oddiy yuk |
| Stress | 0→50→100→0 | 9min | Yuqori yuk |
| Spike | 0→150→0 | 50s | To'satdan yuk oshishi |

## Thresholds (Chegaralar)

| Metrika | Chegara |
|---------|---------|
| HTTP so'rov vaqti (p95) | < 2000ms |
| HTTP so'rov vaqti (p99) | < 5000ms |
| HTTP xatolik darajasi | < 5% |
| Listing so'rov vaqti (p95) | < 1500ms |
| Qidiruv so'rov vaqti (p95) | < 2000ms |
| Auth so'rov vaqti (p95) | < 1000ms |

## Grafana bilan integratsiya

k6 natijalarini Prometheus/Grafana ga yuborish:

```bash
k6 run \
  -o experimental-prometheus-rw \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
  tests/performance/k6-load-test.js
```
