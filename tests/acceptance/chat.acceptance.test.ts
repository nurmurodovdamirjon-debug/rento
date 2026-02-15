/**
 * AC-004: Chat
 *
 * Feature: Chat
 * - Yangi chat boshlash
 * - Real-time xabar olish
 * - Typing indikator
 * - Xabar o'qilgan (read receipt)
 * - Chat ro'yxati
 */
import { ApiClient, createAuthenticatedClient } from './helpers/api';
import { CHAT_DATA, TEST_PHONES, VALID_LISTING } from './helpers/fixtures';

describe('AC-004: Chat', () => {
  let tenantClient: ApiClient;
  let landlordClient: ApiClient;
  let listingId: string;

  beforeAll(async () => {
    // Ikkala foydalanuvchini autentifikatsiya qilish
    landlordClient = await createAuthenticatedClient(TEST_PHONES.landlord);
    tenantClient = await createAuthenticatedClient(TEST_PHONES.tenant);

    // Landlord e'lon yaratadi
    const listingResp = await landlordClient.createListing(VALID_LISTING);
    listingId = listingResp.data.data.id;
  });

  describe('Scenario: Yangi chat boshlash', () => {
    it('should create new chat room for a listing', async () => {
      // Given: Ijarachi e'lon batafsil sahifasida
      // When: "Xabar yuborish" tugmasini bosadi va xabar yozadi
      const response = await tenantClient.createChat({
        listingId,
        initialMessage: CHAT_DATA.initialMessage,
      });

      // Then: Yangi chat xonasi yaratiladi
      expect(response.status).toBe(201);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('roomId');
      expect(response.data.data.roomId).toBeTruthy();
    });

    it('should not create duplicate chat for same listing+users', async () => {
      // Yana bir xil chat yaratishga harakat
      const response = await tenantClient.createChat({
        listingId,
        initialMessage: 'Qayta urinish',
      });

      // Mavjud chat qaytarilishi kerak
      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('roomId');
    });
  });

  describe('Scenario: Xabar yuborish va olish (REST)', () => {
    let roomId: string;

    beforeAll(async () => {
      const chatResp = await tenantClient.createChat({
        listingId,
        initialMessage: CHAT_DATA.initialMessage,
      });
      roomId = chatResp.data.data.roomId;
    });

    it('should send message via REST API', async () => {
      const response = await tenantClient.sendMessage(roomId, {
        content: CHAT_DATA.secondMessage,
        messageType: 'text',
      });

      expect(response.status).toBe(201);
      expect(response.data.success).toBe(true);
      expect(response.data.data).toHaveProperty('id');
      expect(response.data.data.content).toBe(CHAT_DATA.secondMessage);
      expect(response.data.data.messageType).toBe('text');
    });

    it('should get messages for a room', async () => {
      const response = await tenantClient.getMessages(roomId, { page: 1, perPage: 20 });

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.items).toBeInstanceOf(Array);
      expect(response.data.data.items.length).toBeGreaterThan(0);

      // Xabar tuzilmasini tekshirish
      const msg = response.data.data.items[0];
      expect(msg).toHaveProperty('id');
      expect(msg).toHaveProperty('roomId');
      expect(msg).toHaveProperty('senderId');
      expect(msg).toHaveProperty('content');
      expect(msg).toHaveProperty('messageType');
      expect(msg).toHaveProperty('createdAt');
    });

    it('landlord should also see messages', async () => {
      const response = await landlordClient.getMessages(roomId, { page: 1, perPage: 20 });

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.items.length).toBeGreaterThan(0);
    });
  });

  describe('Scenario: Chat ro\'yxati', () => {
    it('should list chats for tenant', async () => {
      const response = await tenantClient.getChats({ page: 1, perPage: 20 });

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.items).toBeInstanceOf(Array);
      expect(response.data.data.items.length).toBeGreaterThan(0);

      const chat = response.data.data.items[0];
      expect(chat).toHaveProperty('roomId');
      expect(chat).toHaveProperty('listing');
      expect(chat).toHaveProperty('otherUser');
      expect(chat).toHaveProperty('unreadCount');
    });

    it('should list chats for landlord', async () => {
      const response = await landlordClient.getChats({ page: 1, perPage: 20 });

      expect(response.status).toBe(200);
      expect(response.data.success).toBe(true);
      expect(response.data.data.items.length).toBeGreaterThan(0);
    });
  });

  describe('Scenario: Xabar o\'qilgan belgilash', () => {
    it('should mark messages as read', async () => {
      // Oldindan chat yaratish
      const chatResp = await tenantClient.createChat({
        listingId,
        initialMessage: 'Test read receipt',
      });
      const roomId = chatResp.data.data.roomId;

      // O'qilgan belgilash
      const response = await landlordClient.markAsRead(roomId);
      expect(response.status).toBe(200);
    });
  });

  describe('Scenario: Autentifikatsiyasiz chat', () => {
    it('should reject unauthenticated chat access', async () => {
      const unauthClient = new ApiClient();
      try {
        await unauthClient.getChats();
        fail('Should have thrown');
      } catch (error: any) {
        expect(error.response.status).toBe(401);
      }
    });
  });
});
