/**
 * AC-007: Moderatsiya
 *
 * Feature: E'lon moderatsiyasi (Admin)
 * - Pending e'lonlar ro'yxati
 * - E'lonni tasdiqlash
 * - E'lonni rad etish
 * - Foydalanuvchini bloklash
 * - Ruxsatsiz kirish
 */
import { ApiClient, createAuthenticatedClient } from './helpers/api';
import { TEST_PHONES, VALID_LISTING } from './helpers/fixtures';

describe('AC-007: Moderatsiya', () => {
  let adminClient: ApiClient;
  let landlordClient: ApiClient;
  let tenantClient: ApiClient;
  let pendingListingId: string;

  beforeAll(async () => {
    // Admin, Landlord, Tenant clientlarini yaratish
    adminClient = await createAuthenticatedClient(TEST_PHONES.admin);
    landlordClient = await createAuthenticatedClient(TEST_PHONES.landlord);
    tenantClient = await createAuthenticatedClient(TEST_PHONES.tenant);

    // Landlord pending e'lon yaratadi
    const listingResp = await landlordClient.createListing(VALID_LISTING);
    pendingListingId = listingResp.data.data.id;
  });

  describe('Scenario: Pending e\'lonlar ro\'yxati', () => {
    it('should list pending listings for admin', async () => {
      // Given: Admin tizimga kirgan
      // When: Pending e'lonlar ro'yxatini so'raydi
      const response = await adminClient.getPendingListings({
        status: 'pending',
        page: 1,
        perPage: 20,
      });

      // Then: Pending e'lonlar ko'rsatiladi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.items).toBeInstanceOf(Array);
    });
  });

  describe('Scenario: E\'lonni tasdiqlash', () => {
    it('should approve a pending listing', async () => {
      // Given: Admin pending e'lonni ko'rmoqda
      // When: "Tasdiqlash" tugmasini bosadi
      const response = await adminClient.approveListing(pendingListingId);

      // Then: E'lon "active" statusga o'tadi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);

      // Verify status change
      const listing = await landlordClient.getListing(pendingListingId);
      expect(listing.data.data.status).toBe('active');
    });
  });

  describe('Scenario: E\'lonni rad etish', () => {
    let rejectListingId: string;

    beforeAll(async () => {
      const resp = await landlordClient.createListing({
        ...VALID_LISTING,
        title: 'Rad etish uchun e\'lon',
      });
      rejectListingId = resp.data.data.id;
    });

    it('should reject a pending listing with reason', async () => {
      // Given: Admin pending e'lonni ko'rmoqda
      // When: "Rad etish" tugmasini bosadi va sababni kiritadi
      const response = await adminClient.rejectListing(rejectListingId, {
        reason: 'Rasm sifati past, qayta yuklang',
      });

      // Then: E'lon "rejected" statusga o'tadi
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);

      // Verify status and rejection reason
      const listing = await landlordClient.getListing(rejectListingId);
      expect(listing.data.data.status).toBe('rejected');
      expect(listing.data.data.rejectionReason).toBeTruthy();
    });
  });

  describe('Scenario: Admin bo\'lmagan foydalanuvchi', () => {
    it('should deny non-admin access to admin endpoints', async () => {
      // Given: Oddiy foydalanuvchi
      // When: Admin endpointiga kiradi
      try {
        await tenantClient.getPendingListings();
        fail('Should have thrown');
      } catch (error: any) {
        // Then: 403 Forbidden
        expect(error.response.status).toBe(403);
      }
    });

    it('should deny tenant from approving listings', async () => {
      try {
        await tenantClient.approveListing(pendingListingId);
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(403);
      }
    });
  });

  describe('Scenario: Autentifikatsiyasiz admin ruxsat', () => {
    it('should deny unauthenticated admin access', async () => {
      const unauthClient = new ApiClient();
      try {
        await unauthClient.getPendingListings();
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(401);
      }
    });
  });
});
