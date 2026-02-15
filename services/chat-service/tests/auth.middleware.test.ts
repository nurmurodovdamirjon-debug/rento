/**
 * Auth middleware unit testlari.
 * JWT mock qilinadi.
 */
import { NextFunction, Request, Response } from 'express';
import jwt from 'jsonwebtoken';
import { authMiddleware, socketAuthMiddleware } from '../src/middleware/auth.middleware';
import { AuthTokenExpiredError, AuthTokenInvalidError } from '../src/utils/errors';

jest.mock('../src/config', () => ({
  config: { jwt: { accessSecret: 'test-secret-key' } },
}));

jest.mock('../src/utils/logger', () => ({
  logger: { info: jest.fn(), debug: jest.fn(), warn: jest.fn(), error: jest.fn() },
}));

// ——— Yordamchi ———
function mockReq(authHeader?: string): Partial<Request> {
  return {
    headers: authHeader ? { authorization: authHeader } : {},
  };
}
const mockRes = {} as Response;
const mockNext = jest.fn() as NextFunction;

beforeEach(() => {
  jest.clearAllMocks();
});

// ————————————————————————————————————
// HTTP authMiddleware
// ————————————————————————————————————
describe('authMiddleware (HTTP)', () => {
  it("to'g'ri token — user set qilinishi", () => {
    const payload = { sub: 'user-123', role: 'tenant', phone: '+998901234567' };
    const token = jwt.sign(payload, 'test-secret-key', { expiresIn: '1h' });

    authMiddleware(mockReq(`Bearer ${token}`) as any, mockRes, mockNext);

    expect(mockNext).toHaveBeenCalledWith();
    const req = mockReq(`Bearer ${token}`) as any;
    authMiddleware(req, mockRes, jest.fn());
    expect(req.user).toBeDefined();
    expect(req.user.id).toBe('user-123');
    expect(req.user.role).toBe('tenant');
  });

  it("header yo'q — AuthTokenInvalidError", () => {
    authMiddleware(mockReq() as any, mockRes, mockNext);
    expect(mockNext).toHaveBeenCalledWith(expect.any(AuthTokenInvalidError));
  });

  it("noto'g'ri format (Bearer yo'q) — AuthTokenInvalidError", () => {
    authMiddleware(mockReq('Token abc') as any, mockRes, mockNext);
    expect(mockNext).toHaveBeenCalledWith(expect.any(AuthTokenInvalidError));
  });

  it("noto'g'ri secret — AuthTokenInvalidError", () => {
    const token = jwt.sign({ sub: 'x', role: 'y', phone: 'z' }, 'wrong-secret');
    authMiddleware(mockReq(`Bearer ${token}`) as any, mockRes, mockNext);
    expect(mockNext).toHaveBeenCalledWith(expect.any(AuthTokenInvalidError));
  });

  it("muddati o'tgan token — AuthTokenExpiredError", () => {
    const token = jwt.sign({ sub: 'x', role: 'y', phone: 'z' }, 'test-secret-key', { expiresIn: '-1s' });
    authMiddleware(mockReq(`Bearer ${token}`) as any, mockRes, mockNext);
    expect(mockNext).toHaveBeenCalledWith(expect.any(AuthTokenExpiredError));
  });
});

// ————————————————————————————————————
// Socket.IO socketAuthMiddleware
// ————————————————————————————————————
describe('socketAuthMiddleware (WebSocket)', () => {
  function mockSocket(token?: string): any {
    return {
      handshake: {
        query: token ? { token } : {},
      },
      data: {},
    };
  }

  it("to'g'ri token — socket.data ga user yozilishi", () => {
    const payload = { sub: 'user-456', role: 'landlord', phone: '+998909876543' };
    const token = jwt.sign(payload, 'test-secret-key', { expiresIn: '1h' });
    const socket = mockSocket(token);

    socketAuthMiddleware(socket, mockNext);

    expect(mockNext).toHaveBeenCalledWith();
    expect(socket.data.userId).toBe('user-456');
    expect(socket.data.role).toBe('landlord');
  });

  it("token yo'q — AuthTokenInvalidError", () => {
    const socket = mockSocket();
    socketAuthMiddleware(socket, mockNext);
    expect(mockNext).toHaveBeenCalledWith(expect.any(AuthTokenInvalidError));
  });

  it("muddati o'tgan token — AuthTokenExpiredError", () => {
    const token = jwt.sign({ sub: 'x', role: 'y', phone: 'z' }, 'test-secret-key', { expiresIn: '-1s' });
    const socket = mockSocket(token);
    socketAuthMiddleware(socket, mockNext);
    expect(mockNext).toHaveBeenCalledWith(expect.any(AuthTokenExpiredError));
  });
});
