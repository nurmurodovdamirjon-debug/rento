/**
 * AC-005: Sevimlilar
 *
 * Feature: Sevimlilar ro'yxati
 * - E'lonni sevimliga qo'shish
 * - Sevimlilar ro'yxatini ko'rish
 * - Sevimlilardan olib tashlash
 * - Sevimli ID lar
 * - Tekshirish
 */
import { ApiClient, createAuthenticatedClient } from './helpers/api';
import { TEST_PHONES, VALID_LISTING } from './helpers/fixtures';

describe('AC-005: Sevimlilar', () => {
  let client: ApiClient;
  let landlordClient: ApiClient;
  let listingId: string;

  beforeAll(async () => {
    // Landlord e'lon yaratadi
    landlordClient = await createAuthenticatedClient(TEST_PHONES.landlord);
    const listingResp = await landlordClient.createListing(VALID_LISTING);
    listingId = listingResp.data.data.id;

    // Tenant — sevimlilar bilan ishlaydi
    client = await createAuthenticatedClient(TEST_PHONES.tenant);
  });

  describe('Scenario: E\'lonni sevimliga qo\'shish', () => {
    it('should toggle favorite ON for a listing', async () => {
      // Given: Foydalanuvchi e'lon batafsil sahifasida
      // When: "Sevimli" tugmasini bosadi
      const response = await client.toggleFavorite(listingId);

      // Then: E'lon sevimliga qo'shiladi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.isFavorite).toBe(true);
      expect(response.data.data.favoritesCount).toBeGreaterThan(0);
    });
  });

  describe('Scenario: Sevimli ekanligini tekshirish', () => {
    it('should check if listing is favorited', async () => {
      const response = await client.checkFavorite(listingId);

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.isFavorite).toBe(true);
    });
  });

  describe('Scenario: Sevimli ID lar', () => {
    it('should return list of favorite listing IDs', async () => {
      const response = await client.getFavoriteIds();

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.listingIds).toBeInstanceOf(Array);
      expect(response.data.data.listingIds).toContain(listingId);
    });
  });

  describe('Scenario: Sevimlilar ro\'yxati', () => {
    it('should return favorites list with listing details', async () => {
      const response = await client.getFavorites({ page: 1, perPage: 20 });

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.items).toBeInstanceOf(Array);
      expect(response.data.data.items.length).toBeGreaterThan(0);

      const favorite = response.data.data.items[0];
      expect(favorite).toHaveProperty('favoriteId');
      expect(favorite).toHaveProperty('listingId');
      expect(favorite).toHaveProperty('title');
      expect(favorite).toHaveProperty('price');
      expect(favorite).toHaveProperty('city');
    });
  });

  describe('Scenario: Sevimlilardan olib tashlash', () => {
    it('should toggle favorite OFF', async () => {
      // When: Yana "Sevimli" tugmasini bosadi
      const response = await client.toggleFavorite(listingId);

      // Then: E'lon sevimlilardan olib tashlanadi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.isFavorite).toBe(false);
    });

    it('should not appear in favorites after removal', async () => {
      const response = await client.checkFavorite(listingId);

      expect(response.status).toBe(200);
      expect(response.data.data.isFavorite).toBe(false);
    });
  });

  describe('Scenario: Autentifikatsiyasiz sevimlilar', () => {
    it('should reject unauthenticated favorites access', async () => {
      const unauthClient = new ApiClient();
      try {
        await unauthClient.toggleFavorite(listingId);
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(401);
      }
    });
  });
});
