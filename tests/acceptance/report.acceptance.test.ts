/**
 * AC-006: Shikoyat (Report)
 *
 * Feature: E'lon/foydalanuvchi haqida shikoyat
 * - Spam shikoyat
 * - Noqonuniy kontentli shikoyat
 * - Firibgarlik shikoyat
 * - Takroriy shikoyat oldini olish
 */
import { ApiClient, createAuthenticatedClient } from './helpers/api';
import { REPORT_DATA, TEST_PHONES, VALID_LISTING } from './helpers/fixtures';

describe('AC-006: Shikoyat (Report)', () => {
  let client: ApiClient;
  let landlordClient: ApiClient;
  let listingId: string;

  beforeAll(async () => {
    // Landlord e'lon yaratadi
    landlordClient = await createAuthenticatedClient(TEST_PHONES.landlord);
    const listingResp = await landlordClient.createListing(VALID_LISTING);
    listingId = listingResp.data.data.id;

    // Tenant — shikoyat qiladi
    client = await createAuthenticatedClient(TEST_PHONES.tenant);
  });

  describe('Scenario: Spam shikoyat', () => {
    it('should create spam report for a listing', async () => {
      // Given: Foydalanuvchi e'lon sahifasida
      // When: "Shikoyat" tugmasini bosadi va sababni tanlaydi
      const response = await client.createReport({
        targetType: 'listing',
        targetId: listingId,
        reason: REPORT_DATA.spam.reason,
        description: REPORT_DATA.spam.description,
      });

      // Then: Shikoyat muvaffaqiyatli yaratiladi
      expect(response.status).toBe(201);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('id');
      expect(response.data.data.reason).toBe('spam');
      expect(response.data.data.status).toBe('pending');
    });
  });

  describe('Scenario: Inappropriate content shikoyat', () => {
    it('should create inappropriate content report', async () => {
      // Yangi listing uchun shikoyat
      const newListing = await landlordClient.createListing({
        ...VALID_LISTING,
        title: 'Boshqa e\'lon shikoyat uchun',
      });

      const response = await client.createReport({
        targetType: 'listing',
        targetId: newListing.data.data.id,
        reason: REPORT_DATA.inappropriate.reason,
        description: REPORT_DATA.inappropriate.description,
      });

      expect(response.status).toBe(201);
      expect(response.data.success).toBe(true);
      expect(response.data.data.reason).toBe('inappropriate_content');
    });
  });

  describe('Scenario: Majburiy maydonlar tekshiruvi', () => {
    it('should reject report without reason', async () => {
      try {
        await client.createReport({
          targetType: 'listing',
          targetId: listingId,
          description: 'Sabab ko\'rsatilmagan',
        });
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(400);
      }
    });

    it('should reject report without target', async () => {
      try {
        await client.createReport({
          reason: 'spam',
          description: 'Target yo\'q',
        });
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(400);
      }
    });
  });

  describe('Scenario: Autentifikatsiyasiz shikoyat', () => {
    it('should reject unauthenticated report', async () => {
      const unauthClient = new ApiClient();
      try {
        await unauthClient.createReport({
          targetType: 'listing',
          targetId: listingId,
          reason: 'spam',
          description: 'Test',
        });
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(401);
      }
    });
  });
});
