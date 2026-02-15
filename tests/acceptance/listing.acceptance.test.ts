/**
 * AC-002: E'lon Yaratish
 *
 * Feature: E'lon yaratish
 * - Muvaffaqiyatli e'lon yaratish
 * - Rasmlar limiti
 * - Majburiy maydonlar to'ldirilmagan
 * - E'lon yangilash va o'chirish
 * - Kunlik limit
 */
import { ApiClient, createAuthenticatedClient } from './helpers/api';
import {
    INVALID_LISTING_NO_PRICE,
    MINIMAL_LISTING,
    TEST_PHONES,
    VALID_LISTING,
} from './helpers/fixtures';

describe('AC-002: E\'lon Yaratish', () => {
  let client: ApiClient;

  beforeAll(async () => {
    client = await createAuthenticatedClient(TEST_PHONES.landlord);
  });

  describe('Scenario: Muvaffaqiyatli e\'lon yaratish', () => {
    it('should create listing with all fields', async () => {
      // Given: Foydalanuvchi tizimga kirgan (authenticated)
      // When: Barcha kerakli maydonlarni to'ldiradi
      const response = await client.createListing(VALID_LISTING);

      // Then: E'lon "pending" statusida yaratiladi
      expect(response.status).toBe(201);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('id');
      expect(response.data.data.status).toBe('pending');
      expect(response.data.data.title).toBe(VALID_LISTING.title);
      expect(response.data.data.type).toBe(VALID_LISTING.type);
      expect(response.data.data.price).toBe(VALID_LISTING.price);
      expect(response.data.data.city).toBe(VALID_LISTING.city);
    });

    it('should create listing with minimal fields', async () => {
      const response = await client.createListing(MINIMAL_LISTING);

      expect(response.status).toBe(201);
      expect(response.data.success).toBe(true);
      expect(response.data.data.status).toBe('pending');
    });
  });

  describe('Scenario: Majburiy maydonlar to\'ldirilmagan', () => {
    it('should reject listing without price', async () => {
      // Given: Foydalanuvchi e'lon yaratmoqda
      // When: Narx maydonini bo'sh qoldiradi
      try {
        await client.createListing(INVALID_LISTING_NO_PRICE);
        fail('Should have thrown');
      } catch (error: any) {
        // Then: Validatsiya xatosi chiqadi
        expect(error.response.status).toBe(400);
        expect(error.response.data.success).toBe(false);
      }
    });

    it('should reject listing without type', async () => {
      try {
        await client.createListing({ price: 5000000, city: 'tashkent', title: 'test' });
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(400);
      }
    });

    it('should reject listing without title', async () => {
      try {
        await client.createListing({ type: 'apartment', price: 5000000, city: 'tashkent' });
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(400);
      }
    });
  });

  describe('Scenario: E\'lon olish va yangilash', () => {
    let listingId: string;

    beforeAll(async () => {
      const response = await client.createListing(VALID_LISTING);
      listingId = response.data.data.id;
    });

    it('should get listing by ID', async () => {
      const response = await client.getListing(listingId);

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.id).toBe(listingId);
      expect(response.data.data.title).toBe(VALID_LISTING.title);
    });

    it('should update listing fields', async () => {
      const updates = {
        title: 'Yangilangan sarlavha',
        price: 6000000,
        rooms: 3,
      };

      const response = await client.updateListing(listingId, updates);

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.title).toBe('Yangilangan sarlavha');
      expect(response.data.data.price).toBe(6000000);
    });

    it('should get my listings', async () => {
      const response = await client.getMyListings();

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.items).toBeInstanceOf(Array);
      expect(response.data.data.items.length).toBeGreaterThan(0);
    });
  });

  describe('Scenario: E\'lon o\'chirish', () => {
    it('should delete own listing', async () => {
      const createResp = await client.createListing(MINIMAL_LISTING);
      const listingId = createResp.data.data.id;

      const deleteResp = await client.deleteListing(listingId);
      expect(deleteResp.status).toBe(200);

      // Verify deletion
      try {
        await client.getListing(listingId);
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(404);
      }
    });
  });

  describe('Scenario: Autentifikatsiyasiz e\'lon yaratish', () => {
    it('should reject unauthenticated listing creation', async () => {
      const unauthClient = new ApiClient();
      try {
        await unauthClient.createListing(VALID_LISTING);
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(401);
      }
    });
  });
});
