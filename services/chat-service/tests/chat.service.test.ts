/**
 * ChatService unit testlari.
 * DB (pool) va Redis mock qilinadi.
 */

// ——— Mocklar ———
const mockPoolQuery = jest.fn();
const mockPoolConnect = jest.fn();
const mockClientQuery = jest.fn();
const mockClientRelease = jest.fn();

jest.mock('../src/config/database', () => ({
  pool: {
    query: (...args: unknown[]) => mockPoolQuery(...args),
    connect: () =>
      mockPoolConnect().then(() => ({
        query: mockClientQuery,
        release: mockClientRelease,
      })),
  },
}));

const mockRedisGet = jest.fn();
const mockRedisSet = jest.fn();
const mockRedisDel = jest.fn();

jest.mock('../src/config/redis', () => ({
  redis: {
    get: (...args: unknown[]) => mockRedisGet(...args),
    set: (...args: unknown[]) => mockRedisSet(...args),
    del: (...args: unknown[]) => mockRedisDel(...args),
  },
}));

jest.mock('../src/utils/logger', () => ({
  logger: { info: jest.fn(), debug: jest.fn(), warn: jest.fn(), error: jest.fn() },
}));

jest.mock('../src/services/message.service', () => ({
  messageService: { sendMessage: jest.fn() },
}));

import { chatService } from '../src/services/chat.service';
import { messageService } from '../src/services/message.service';
import { ChatRoomNotFoundError, ChatSelfMessageError, ChatUnauthorizedError } from '../src/utils/errors';

// ——— Yordamchi ———
const ROOM_ID = '11111111-1111-1111-1111-111111111111';
const LISTING_ID = '22222222-2222-2222-2222-222222222222';
const TENANT_ID = '33333333-3333-3333-3333-333333333333';
const LANDLORD_ID = '44444444-4444-4444-4444-444444444444';

const sampleRoom = {
  id: ROOM_ID,
  listing_id: LISTING_ID,
  tenant_id: TENANT_ID,
  landlord_id: LANDLORD_ID,
  last_message_at: null,
  is_active: true,
  created_at: '2025-01-01T00:00:00Z',
};

beforeEach(() => {
  jest.clearAllMocks();
  mockPoolConnect.mockResolvedValue(undefined);
});

// ————————————————————————————————————
// createOrGetRoom
// ————————————————————————————————————
describe('ChatService.createOrGetRoom', () => {
  it("yangi xona yaratish — e'lon topilsa", async () => {
    // BEGIN
    mockClientQuery
      .mockResolvedValueOnce(undefined)                                   // BEGIN
      .mockResolvedValueOnce({ rows: [{ user_id: LANDLORD_ID }] })        // listings SELECT
      .mockResolvedValueOnce({ rows: [] })                                // existing room check
      .mockResolvedValueOnce({ rows: [sampleRoom] })                      // INSERT RETURNING
      .mockResolvedValueOnce(undefined);                                  // COMMIT

    (messageService.sendMessage as jest.Mock).mockResolvedValueOnce({});

    const result = await chatService.createOrGetRoom(TENANT_ID, LISTING_ID, 'Salom!');
    expect(result.isNew).toBe(true);
    expect(result.room.id).toBe(ROOM_ID);
    expect(messageService.sendMessage).toHaveBeenCalledWith(ROOM_ID, TENANT_ID, {
      content: 'Salom!',
      message_type: 'text',
    });
  });

  it("mavjud xonani qaytarish (isNew = false)", async () => {
    mockClientQuery
      .mockResolvedValueOnce(undefined)                                   // BEGIN
      .mockResolvedValueOnce({ rows: [{ user_id: LANDLORD_ID }] })        // listings
      .mockResolvedValueOnce({ rows: [sampleRoom] })                      // existing room TOPILDI
      .mockResolvedValueOnce(undefined);                                  // COMMIT

    (messageService.sendMessage as jest.Mock).mockResolvedValueOnce({});

    const result = await chatService.createOrGetRoom(TENANT_ID, LISTING_ID, 'Test');
    expect(result.isNew).toBe(false);
    expect(result.room.id).toBe(ROOM_ID);
  });

  it("o'ziga o'zi xabar — ChatSelfMessageError", async () => {
    mockClientQuery
      .mockResolvedValueOnce(undefined)                                   // BEGIN
      .mockResolvedValueOnce({ rows: [{ user_id: TENANT_ID }] });         // listings — tenant = landlord

    await expect(chatService.createOrGetRoom(TENANT_ID, LISTING_ID, 'Test'))
      .rejects
      .toThrow(ChatSelfMessageError);

    // ROLLBACK chaqirilganini tekshirish
    expect(mockClientQuery).toHaveBeenCalledWith('ROLLBACK');
  });

  it("e'lon topilmadi — ChatRoomNotFoundError", async () => {
    mockClientQuery
      .mockResolvedValueOnce(undefined)                                   // BEGIN
      .mockResolvedValueOnce({ rows: [] });                               // listing topilmadi

    await expect(chatService.createOrGetRoom(TENANT_ID, LISTING_ID, 'X'))
      .rejects
      .toThrow(ChatRoomNotFoundError);
  });
});

// ————————————————————————————————————
// getRoom
// ————————————————————————————————————
describe('ChatService.getRoom', () => {
  it("mavjud xona + huquqli foydalanuvchi", async () => {
    mockPoolQuery.mockResolvedValueOnce({ rows: [sampleRoom] });

    const room = await chatService.getRoom(ROOM_ID, TENANT_ID);
    expect(room.id).toBe(ROOM_ID);
  });

  it("xona topilmadi — ChatRoomNotFoundError", async () => {
    mockPoolQuery.mockResolvedValueOnce({ rows: [] });

    await expect(chatService.getRoom(ROOM_ID, TENANT_ID))
      .rejects
      .toThrow(ChatRoomNotFoundError);
  });

  it("huquqsiz foydalanuvchi — ChatUnauthorizedError", async () => {
    mockPoolQuery.mockResolvedValueOnce({ rows: [sampleRoom] });

    await expect(chatService.getRoom(ROOM_ID, '99999999-9999-9999-9999-999999999999'))
      .rejects
      .toThrow(ChatUnauthorizedError);
  });
});

// ————————————————————————————————————
// Online/Offline
// ————————————————————————————————————
describe('ChatService online/offline', () => {
  it('setUserOnline — Redis SET bilan 300s TTL', async () => {
    mockRedisSet.mockResolvedValueOnce('OK');
    await chatService.setUserOnline(TENANT_ID);
    expect(mockRedisSet).toHaveBeenCalledWith(`user:online:${TENANT_ID}`, '1', 'EX', 300);
  });

  it('setUserOffline — Redis DEL + DB update', async () => {
    mockRedisDel.mockResolvedValueOnce(1);
    mockPoolQuery.mockResolvedValueOnce(undefined);

    await chatService.setUserOffline(TENANT_ID);
    expect(mockRedisDel).toHaveBeenCalledWith(`user:online:${TENANT_ID}`);
    expect(mockPoolQuery).toHaveBeenCalledWith(
      'UPDATE users SET last_seen_at = NOW() WHERE id = $1',
      [TENANT_ID]
    );
  });

  it("isUserOnline — '1' bo'lsa true", async () => {
    mockRedisGet.mockResolvedValueOnce('1');
    const result = await chatService.isUserOnline(TENANT_ID);
    expect(result).toBe(true);
  });

  it("isUserOnline — null bo'lsa false", async () => {
    mockRedisGet.mockResolvedValueOnce(null);
    const result = await chatService.isUserOnline(TENANT_ID);
    expect(result).toBe(false);
  });
});
