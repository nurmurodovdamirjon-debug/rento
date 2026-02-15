/**
 * MessageService unit testlari.
 * DB (pool) mock qilinadi.
 */

const mockPoolQuery = jest.fn();
const mockClientQuery = jest.fn();
const mockClientRelease = jest.fn();

jest.mock('../src/config/database', () => ({
  pool: {
    query: (...args: unknown[]) => mockPoolQuery(...args),
    connect: jest.fn().mockResolvedValue({
      query: (...args: unknown[]) => mockClientQuery(...args),
      release: mockClientRelease,
    }),
  },
}));

jest.mock('../src/utils/logger', () => ({
  logger: { info: jest.fn(), debug: jest.fn(), warn: jest.fn(), error: jest.fn() },
}));

import { messageService } from '../src/services/message.service';

const ROOM_ID = '11111111-1111-1111-1111-111111111111';
const SENDER_ID = '33333333-3333-3333-3333-333333333333';

const sampleMessage = {
  id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  room_id: ROOM_ID,
  sender_id: SENDER_ID,
  content: 'Test xabar',
  message_type: 'text',
  media_url: null,
  metadata: null,
  is_read: false,
  read_at: null,
  created_at: '2025-01-10T12:00:00Z',
};

beforeEach(() => {
  jest.clearAllMocks();
});

// ————————————————————————————————————
// sendMessage
// ————————————————————————————————————
describe('MessageService.sendMessage', () => {
  it('text xabar muvaffaqiyatli yuborilishi', async () => {
    mockClientQuery
      .mockResolvedValueOnce(undefined)                        // BEGIN
      .mockResolvedValueOnce({ rows: [sampleMessage] })        // INSERT RETURNING
      .mockResolvedValueOnce(undefined)                        // UPDATE chat_rooms
      .mockResolvedValueOnce(undefined);                       // COMMIT

    const msg = await messageService.sendMessage(ROOM_ID, SENDER_ID, {
      content: 'Test xabar',
      message_type: 'text',
    });

    expect(msg.id).toBe(sampleMessage.id);
    expect(msg.content).toBe('Test xabar');
    expect(mockClientQuery).toHaveBeenCalledTimes(4);
  });

  it('image xabar media_url bilan', async () => {
    const imageMsg = { ...sampleMessage, message_type: 'image', media_url: 'https://cdn.rento.uz/img.webp', content: null };
    mockClientQuery
      .mockResolvedValueOnce(undefined)
      .mockResolvedValueOnce({ rows: [imageMsg] })
      .mockResolvedValueOnce(undefined)
      .mockResolvedValueOnce(undefined);

    const msg = await messageService.sendMessage(ROOM_ID, SENDER_ID, {
      message_type: 'image',
      media_url: 'https://cdn.rento.uz/img.webp',
    });

    expect(msg.message_type).toBe('image');
    expect(msg.media_url).toBe('https://cdn.rento.uz/img.webp');
  });

  it('DB xato bo\'lsa ROLLBACK qilinishi', async () => {
    mockClientQuery
      .mockResolvedValueOnce(undefined)                        // BEGIN
      .mockRejectedValueOnce(new Error('DB error'));           // INSERT fails

    await expect(
      messageService.sendMessage(ROOM_ID, SENDER_ID, { content: 'X', message_type: 'text' })
    ).rejects.toThrow('DB error');

    expect(mockClientQuery).toHaveBeenCalledWith('ROLLBACK');
  });
});

// ————————————————————————————————————
// getMessages
// ————————————————————————————————————
describe('MessageService.getMessages', () => {
  it('sahifalash bilan xabarlarni olish', async () => {
    mockPoolQuery
      .mockResolvedValueOnce({ rows: [{ count: '5' }] })                 // COUNT
      .mockResolvedValueOnce({ rows: [sampleMessage, sampleMessage] });   // SELECT

    const result = await messageService.getMessages(ROOM_ID, 1, 20);
    expect(result.total).toBe(5);
    expect(result.messages).toHaveLength(2);
  });
});

// ————————————————————————————————————
// markAsRead
// ————————————————————————————————————
describe('MessageService.markAsRead', () => {
  it("boshqa foydalanuvchining xabarlarini o'qildi deb belgilash", async () => {
    mockPoolQuery.mockResolvedValueOnce({ rowCount: 3 });

    const count = await messageService.markAsRead(ROOM_ID, SENDER_ID);
    expect(count).toBe(3);
  });

  it("o'qilmagan xabar yo'q bo'lsa 0", async () => {
    mockPoolQuery.mockResolvedValueOnce({ rowCount: 0 });

    const count = await messageService.markAsRead(ROOM_ID, SENDER_ID);
    expect(count).toBe(0);
  });
});

// ————————————————————————————————————
// markAsReadUpTo
// ————————————————————————————————————
describe('MessageService.markAsReadUpTo', () => {
  it("berilgan xabargacha o'qildi belgilash", async () => {
    mockPoolQuery.mockResolvedValueOnce({ rowCount: 2 });

    const count = await messageService.markAsReadUpTo(ROOM_ID, SENDER_ID, sampleMessage.id);
    expect(count).toBe(2);
  });
});
