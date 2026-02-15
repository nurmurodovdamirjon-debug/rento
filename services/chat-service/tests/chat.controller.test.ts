/**
 * ChatController unit testlari.
 * Service va message service mock qilinadi, supertest bilan integration test.
 */
import express, { NextFunction, Response } from 'express';
import request from 'supertest';
import { chatController } from '../src/controllers/chat.controller';
import { AuthRequest } from '../src/types';

// ——— Service mocklar ———
const mockCreateOrGetRoom = jest.fn();
const mockGetRooms = jest.fn();
const mockGetRoom = jest.fn();

jest.mock('../src/services/chat.service', () => ({
  chatService: {
    createOrGetRoom: (...args: unknown[]) => mockCreateOrGetRoom(...args),
    getRooms: (...args: unknown[]) => mockGetRooms(...args),
    getRoom: (...args: unknown[]) => mockGetRoom(...args),
  },
}));

const mockSendMessage = jest.fn();
const mockGetMessages = jest.fn();
const mockMarkAsRead = jest.fn();

jest.mock('../src/services/message.service', () => ({
  messageService: {
    sendMessage: (...args: unknown[]) => mockSendMessage(...args),
    getMessages: (...args: unknown[]) => mockGetMessages(...args),
    markAsRead: (...args: unknown[]) => mockMarkAsRead(...args),
  },
}));

jest.mock('../src/utils/logger', () => ({
  logger: { info: jest.fn(), debug: jest.fn(), warn: jest.fn(), error: jest.fn() },
}));

// ——— Test Express app ———
function createTestApp() {
  const app = express();
  app.use(express.json());

  // Fake auth middleware — har doim user set qiladi
  app.use((req: AuthRequest, _res: Response, next: NextFunction) => {
    req.user = { id: 'user-001', role: 'tenant', phone: '+998901234567' };
    next();
  });

  app.post('/chats', (req, res, next) => chatController.createChat(req as AuthRequest, res, next));
  app.get('/chats', (req, res, next) => chatController.getChats(req as AuthRequest, res, next));
  app.get('/chats/:room_id/messages', (req, res, next) => chatController.getMessages(req as AuthRequest, res, next));
  app.post('/chats/:room_id/messages', (req, res, next) => chatController.sendMessage(req as AuthRequest, res, next));
  app.put('/chats/:room_id/read', (req, res, next) => chatController.markAsRead(req as AuthRequest, res, next));

  // Error handler
  app.use((err: any, _req: express.Request, res: express.Response, _next: NextFunction) => {
    res.status(err.statusCode || 500).json({
      success: false,
      error: { code: err.code || 'INTERNAL_ERROR', message: err.message },
    });
  });

  return app;
}

const app = createTestApp();

const ROOM_ID = '11111111-1111-4111-a111-111111111111';
const LISTING_ID = '22222222-2222-4222-a222-222222222222';

const sampleRoom = {
  id: ROOM_ID,
  listing_id: LISTING_ID,
  tenant_id: 'user-001',
  landlord_id: 'user-002',
  is_active: true,
  created_at: '2025-01-01T00:00:00Z',
};

const sampleRoomListItem = {
  room_id: ROOM_ID,
  listing: { id: LISTING_ID, title: 'Test kvartira', image_url: null },
  other_user: { id: 'user-002', full_name: 'Test User', avatar_url: null, is_online: false, last_seen_at: null },
  last_message: { content: 'Salom', sender_id: 'user-001', created_at: '2025-01-01T00:00:00Z' },
  unread_count: 0,
  created_at: '2025-01-01T00:00:00Z',
};

beforeEach(() => {
  jest.clearAllMocks();
});

// ————————————————————————————————————
// POST /chats — createChat
// ————————————————————————————————————
describe('POST /chats', () => {
  it('yangi chat yaratish — 201', async () => {
    mockCreateOrGetRoom.mockResolvedValueOnce({ room: sampleRoom, isNew: true });
    mockGetRooms.mockResolvedValueOnce({ rooms: [sampleRoomListItem], total: 1 });

    const res = await request(app)
      .post('/chats')
      .send({ listing_id: LISTING_ID, initial_message: 'Salom!' });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.room_id).toBe(ROOM_ID);
  });

  it('mavjud chat — 200', async () => {
    mockCreateOrGetRoom.mockResolvedValueOnce({ room: sampleRoom, isNew: false });
    mockGetRooms.mockResolvedValueOnce({ rooms: [sampleRoomListItem], total: 1 });

    const res = await request(app)
      .post('/chats')
      .send({ listing_id: LISTING_ID, initial_message: 'Salom!' });

    expect(res.status).toBe(200);
  });

  it("noto'g'ri body — 400 validation error", async () => {
    const res = await request(app)
      .post('/chats')
      .send({ listing_id: 'not-uuid' });

    expect(res.status).toBe(400);
    expect(res.body.error.code).toBe('VALIDATION_ERROR');
  });
});

// ————————————————————————————————————
// GET /chats — getChats
// ————————————————————————————————————
describe('GET /chats', () => {
  it("ro'yxatni olish — sahifalash bilan", async () => {
    mockGetRooms.mockResolvedValueOnce({ rooms: [sampleRoomListItem], total: 1 });

    const res = await request(app).get('/chats').query({ page: 1, per_page: 20 });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toHaveLength(1);
    expect(res.body.meta.total).toBe(1);
    expect(res.body.meta.per_page).toBe(20);
  });

  it('per_page > 50 limitlanishi', async () => {
    mockGetRooms.mockResolvedValueOnce({ rooms: [], total: 0 });

    const res = await request(app).get('/chats').query({ per_page: 100 });

    expect(res.status).toBe(200);
    expect(res.body.meta.per_page).toBe(50);
  });
});

// ————————————————————————————————————
// GET /chats/:room_id/messages — getMessages
// ————————————————————————————————————
describe('GET /chats/:room_id/messages', () => {
  const sampleMsg = {
    id: 'msg-001',
    room_id: ROOM_ID,
    sender_id: 'user-001',
    content: 'Hello',
    message_type: 'text',
    is_read: false,
    created_at: '2025-01-01T12:00:00Z',
  };

  it('xabarlar tarixi', async () => {
    mockGetRoom.mockResolvedValueOnce(sampleRoom);
    mockGetMessages.mockResolvedValueOnce({ messages: [sampleMsg], total: 1 });

    const res = await request(app).get(`/chats/${ROOM_ID}/messages`);

    expect(res.status).toBe(200);
    expect(res.body.data).toHaveLength(1);
    expect(res.body.meta.total).toBe(1);
  });
});

// ————————————————————————————————————
// POST /chats/:room_id/messages — sendMessage
// ————————————————————————————————————
describe('POST /chats/:room_id/messages', () => {
  it('text xabar yuborish — 201', async () => {
    mockGetRoom.mockResolvedValueOnce(sampleRoom);
    mockSendMessage.mockResolvedValueOnce({
      id: 'msg-002',
      room_id: ROOM_ID,
      sender_id: 'user-001',
      content: 'Test xabar',
      message_type: 'text',
      created_at: '2025-01-01T12:00:00Z',
    });

    const res = await request(app)
      .post(`/chats/${ROOM_ID}/messages`)
      .send({ content: 'Test xabar', message_type: 'text' });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.content).toBe('Test xabar');
  });

  it("noto'g'ri message_type — 400", async () => {
    const res = await request(app)
      .post(`/chats/${ROOM_ID}/messages`)
      .send({ content: 'X', message_type: 'video' });

    expect(res.status).toBe(400);
  });
});

// ————————————————————————————————————
// PUT /chats/:room_id/read — markAsRead
// ————————————————————————————————————
describe('PUT /chats/:room_id/read', () => {
  it("o'qildi belgilash — 204", async () => {
    mockGetRoom.mockResolvedValueOnce(sampleRoom);
    mockMarkAsRead.mockResolvedValueOnce(3);

    const res = await request(app).put(`/chats/${ROOM_ID}/read`);

    expect(res.status).toBe(204);
  });
});
