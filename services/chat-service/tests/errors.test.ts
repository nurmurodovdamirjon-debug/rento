import {
    AppError,
    AuthTokenExpiredError,
    AuthTokenInvalidError,
    ChatRoomNotFoundError,
    ChatSelfMessageError,
    ChatUnauthorizedError,
    RateLimitError,
    ValidationError,
} from '../src/utils/errors';

describe('Custom Errors', () => {
  it('AppError asosiy xususiyatlari', () => {
    const err = new AppError(400, 'TEST_CODE', 'Test xabar', { field: 'value' });
    expect(err.statusCode).toBe(400);
    expect(err.code).toBe('TEST_CODE');
    expect(err.message).toBe('Test xabar');
    expect(err.details).toEqual({ field: 'value' });
    expect(err).toBeInstanceOf(Error);
    expect(err).toBeInstanceOf(AppError);
  });

  it('ChatRoomNotFoundError — 404', () => {
    const err = new ChatRoomNotFoundError();
    expect(err.statusCode).toBe(404);
    expect(err.code).toBe('CHAT_ROOM_NOT_FOUND');
  });

  it('ChatSelfMessageError — 400', () => {
    const err = new ChatSelfMessageError();
    expect(err.statusCode).toBe(400);
    expect(err.code).toBe('CHAT_SELF_MESSAGE');
  });

  it('ChatUnauthorizedError — 403', () => {
    const err = new ChatUnauthorizedError();
    expect(err.statusCode).toBe(403);
    expect(err.code).toBe('CHAT_UNAUTHORIZED');
  });

  it('AuthTokenInvalidError — 401', () => {
    const err = new AuthTokenInvalidError();
    expect(err.statusCode).toBe(401);
    expect(err.code).toBe('AUTH_TOKEN_INVALID');
  });

  it('AuthTokenExpiredError — 401', () => {
    const err = new AuthTokenExpiredError();
    expect(err.statusCode).toBe(401);
    expect(err.code).toBe('AUTH_TOKEN_EXPIRED');
  });

  it('ValidationError — 400 + details', () => {
    const err = new ValidationError('Maydon xato', { name: ['required'] });
    expect(err.statusCode).toBe(400);
    expect(err.code).toBe('VALIDATION_ERROR');
    expect(err.details).toEqual({ name: ['required'] });
  });

  it('RateLimitError — 429', () => {
    const err = new RateLimitError();
    expect(err.statusCode).toBe(429);
    expect(err.code).toBe('RATE_LIMIT_EXCEEDED');
  });
});
