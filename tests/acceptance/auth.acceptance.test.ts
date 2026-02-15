/**
 * AC-001: Ro'yxatdan O'tish (SMS OTP)
 *
 * Feature: Ro'yxatdan o'tish (SMS OTP)
 * - Muvaffaqiyatli ro'yxatdan o'tish
 * - OTP tasdiqlash
 * - Noto'g'ri OTP
 * - OTP muddati tugagan
 * - OTP limitga yetdi
 */
import { ApiClient } from './helpers/api';
import { TEST_OTP, TEST_PHONES } from './helpers/fixtures';

describe('AC-001: Ro\'yxatdan O\'tish (SMS OTP)', () => {
  let client: ApiClient;

  beforeEach(() => {
    client = new ApiClient();
  });

  describe('Scenario: Muvaffaqiyatli ro\'yxatdan o\'tish', () => {
    it('should send OTP when valid phone is provided', async () => {
      // Given: Foydalanuvchi ilovani ochgan
      // When: Foydalanuvchi telefon raqamini kiritadi va "Kodni yuborish" bosadi
      const response = await client.sendOtp(TEST_PHONES.tenant);

      // Then: Tizim 6 raqamli OTP kodni SMS orqali yuboradi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('expiresIn');
    });

    it('should reject invalid phone format', async () => {
      try {
        await client.sendOtp('invalid-phone');
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(400);
        expect(error.response.data.success).toBe(false);
      }
    });
  });

  describe('Scenario: OTP tasdiqlash', () => {
    it('should verify correct OTP and return JWT tokens', async () => {
      // Given: Foydalanuvchi OTP kodni olgan
      await client.sendOtp(TEST_PHONES.tenant);

      // When: To'g'ri 6 raqamli kodni kiritadi
      const response = await client.verifyOtp(TEST_PHONES.tenant, TEST_OTP);

      // Then: Tizim foydalanuvchini ro'yxatdan o'tkazadi va JWT tokenlar beradi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('accessToken');
      expect(response.data.data).toHaveProperty('refreshToken');
      expect(response.data.data.accessToken).toBeTruthy();
      expect(response.data.data.refreshToken).toBeTruthy();
    });

    it('should allow access to protected endpoints with token', async () => {
      // Autentifikatsiya
      await client.sendOtp(TEST_PHONES.tenant);
      const authResp = await client.verifyOtp(TEST_PHONES.tenant, TEST_OTP);
      client.setToken(authResp.data.data.accessToken);

      // Profil olish
      const profileResp = await client.getMyProfile();
      expect(profileResp.status).toBe(200);
      expect(profileResp.data.success).toBe(true);
      expect(profileResp.data.data).toHaveProperty('id');
      expect(profileResp.data.data).toHaveProperty('phone');
    });
  });

  describe('Scenario: Noto\'g\'ri OTP', () => {
    it('should reject wrong OTP code', async () => {
      // Given: Foydalanuvchi OTP kodni olgan
      await client.sendOtp(TEST_PHONES.tenant);

      // When: Noto'g'ri kodni kiritadi
      try {
        await client.verifyOtp(TEST_PHONES.tenant, '000000');
        fail('Should have thrown');
      } catch (error: any) {
        // Then: Xato xabari chiqadi
        expect(error.response.status).toBe(400);
        expect(error.response.data.success).toBe(false);
        expect(error.response.data.error.message).toBeTruthy();
      }
    });
  });

  describe('Scenario: Token yangilash', () => {
    it('should refresh access token with valid refresh token', async () => {
      // Login
      await client.sendOtp(TEST_PHONES.tenant);
      const authResp = await client.verifyOtp(TEST_PHONES.tenant, TEST_OTP);
      const refreshToken = authResp.data.data.refreshToken;

      // When: Refresh token yuboradi
      const response = await client.refreshToken(refreshToken);

      // Then: Yangi access token olinadi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('accessToken');
    });
  });

  describe('Scenario: Chiqish (Logout)', () => {
    it('should logout successfully', async () => {
      // Login
      await client.sendOtp(TEST_PHONES.tenant);
      const authResp = await client.verifyOtp(TEST_PHONES.tenant, TEST_OTP);
      client.setToken(authResp.data.data.accessToken);

      // When: Chiqish
      const response = await client.logout();

      // Then: Muvaffaqiyatli
      expect(response.status).toBe(200);
    });
  });

  describe('Scenario: Himoyalangan endpointga tokensiz kirish', () => {
    it('should reject unauthenticated requests', async () => {
      try {
        await client.getMyProfile();
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(401);
      }
    });
  });
});
