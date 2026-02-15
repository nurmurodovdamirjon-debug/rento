/**
 * AC-003: Qidiruv
 *
 * Feature: E'lon qidiruv
 * - Filtr bilan qidiruv
 * - Matnli qidiruv (Elasticsearch)
 * - Bo'sh natija
 * - Pagination
 * - Yaqin atrofdagi e'lonlar
 */
import { ApiClient, createAuthenticatedClient } from './helpers/api';
import { SEARCH_FILTERS, TEST_PHONES } from './helpers/fixtures';

describe('AC-003: Qidiruv', () => {
  let client: ApiClient;

  beforeAll(async () => {
    client = await createAuthenticatedClient(TEST_PHONES.tenant);
  });

  describe('Scenario: Filtr bilan qidiruv', () => {
    it('should return listings matching city filter', async () => {
      // Given: Foydalanuvchi bosh sahifada
      // When: Filtrlarni tanlaydi
      const response = await client.getListings({
        city: 'tashkent',
        page: 1,
        perPage: 20,
      });

      // Then: Mos e'lonlar ro'yxati ko'rsatiladi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('items');
      expect(response.data.data).toHaveProperty('meta');
      expect(response.data.data.meta).toHaveProperty('total');
      expect(response.data.data.meta).toHaveProperty('page');
    });

    it('should filter by type and rooms', async () => {
      const response = await client.getListings({
        city: 'tashkent',
        type: 'apartment',
        rooms: 2,
      });

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      if (response.data.data.items.length > 0) {
        response.data.data.items.forEach((item: any) => {
          expect(item.type).toBe('apartment');
          expect(item.rooms).toBe(2);
        });
      }
    });

    it('should filter by price range', async () => {
      const response = await client.getListings({
        minPrice: 3000000,
        maxPrice: 8000000,
      });

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      if (response.data.data.items.length > 0) {
        response.data.data.items.forEach((item: any) => {
          expect(item.price).toBeGreaterThanOrEqual(3000000);
          expect(item.price).toBeLessThanOrEqual(8000000);
        });
      }
    });
  });

  describe('Scenario: Matnli qidiruv (Elasticsearch)', () => {
    it('should search listings by text query', async () => {
      // Given: Foydalanuvchi qidiruv maydonida
      // When: Matn yozadi
      const response = await client.searchListings({
        query: SEARCH_FILTERS.textSearch.query,
      });

      // Then: Elasticsearch natijalar beradi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('items');
    });
  });

  describe('Scenario: Bo\'sh natija', () => {
    it('should return empty results for non-matching filters', async () => {
      // Given: Foydalanuvchi filtr tanlagan
      // When: Hech qanday mos e'lon topilmasa
      const response = await client.getListings({
        city: 'nonexistentcity999',
        type: 'castle',
      });

      // Then: Bo'sh natija qaytariladi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.items).toHaveLength(0);
      expect(response.data.data.meta.total).toBe(0);
    });
  });

  describe('Scenario: Pagination', () => {
    it('should support paginated results', async () => {
      const page1 = await client.getListings({ page: 1, perPage: 5 });

      expect(page1.status).toBe(200);
      expect(page1.data.data.meta.page).toBe(1);
      expect(page1.data.data.meta.perPage).toBe(5);
      expect(page1.data.data.items.length).toBeLessThanOrEqual(5);
    });

    it('should return different items for different pages', async () => {
      const page1 = await client.getListings({ page: 1, perPage: 2 });
      const page2 = await client.getListings({ page: 2, perPage: 2 });

      if (page1.data.data.items.length > 0 && page2.data.data.items.length > 0) {
        const ids1 = page1.data.data.items.map((i: any) => i.id);
        const ids2 = page2.data.data.items.map((i: any) => i.id);
        const overlap = ids1.filter((id: string) => ids2.includes(id));
        expect(overlap).toHaveLength(0);
      }
    });
  });

  describe('Scenario: Yaqin atrofdagi e\'lonlar', () => {
    it('should return nearby listings with distance', async () => {
      const response = await client.getNearbyListings({
        latitude: 41.2867,
        longitude: 69.2072,
        radiusKm: 5,
      });

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('items');
      if (response.data.data.items.length > 0) {
        expect(response.data.data.items[0]).toHaveProperty('distanceMeters');
      }
    });
  });
});
