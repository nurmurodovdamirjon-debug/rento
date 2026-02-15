# RENTO Acceptance Tests (Gherkin → Avtomatik)

## Tuzilma

```
tests/acceptance/
├── auth.acceptance.test.ts        # AC-001: Ro'yxatdan o'tish (SMS OTP)
├── listing.acceptance.test.ts     # AC-002: E'lon yaratish
├── search.acceptance.test.ts      # AC-003: Qidiruv
├── chat.acceptance.test.ts        # AC-004: Chat
├── favorite.acceptance.test.ts    # AC-005: Sevimlilar
├── report.acceptance.test.ts      # AC-006: Shikoyat/Report
├── moderation.acceptance.test.ts  # AC-007: Moderatsiya
├── helpers/
│   ├── api.ts                     # Base API client
│   └── fixtures.ts                # Test data fixtures
├── jest.config.ts                 # Jest konfiguratsiya
└── README.md
```

## Ishga tushirish

```bash
cd tests/acceptance
npm install
npm test                    # Barcha testlarni ishga tushirish
npm test -- --grep "Auth"   # Faqat Auth testlarini
```

## Muhit o'zgaruvchilari

```env
API_URL=http://localhost:3000
AUTH_URL=http://localhost:3001
CHAT_URL=http://localhost:3003
TEST_PHONE=+998901234567
TEST_OTP=123456
```

## CI/CD integratsiya

GitHub Actions workflow da:
```yaml
- name: Run Acceptance Tests
  run: |
    cd tests/acceptance
    npm ci
    npm test
```
