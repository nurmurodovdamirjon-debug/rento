import { AppError, AuthInvalidPhoneError, AuthOtpExpiredError, AuthOtpInvalidError, AuthOtpLimitError, AuthTokenExpiredError, AuthTokenInvalidError, NotFoundError, RateLimitError, UserBlockedError, ValidationError } from '../src/utils/errors';

describe('Custom Errors', () => {
  describe('AppError', () => {
    it('should create error with correct properties', () => {
      const err = new AppError(400, 'TEST_ERROR', 'Test message', { field: 'test' });
      expect(err.statusCode).toBe(400);
      expect(err.code).toBe('TEST_ERROR');
      expect(err.message).toBe('Test message');
      expect(err.details).toEqual({ field: 'test' });
      expect(err instanceof Error).toBe(true);
      expect(err instanceof AppError).toBe(true);
    });
  });

  describe('Auth Errors', () => {
    it('AuthInvalidPhoneError should be 400', () => {
      const err = new AuthInvalidPhoneError();
      expect(err.statusCode).toBe(400);
      expect(err.code).toBe('AUTH_INVALID_PHONE');
    });

    it('AuthOtpExpiredError should be 400', () => {
      const err = new AuthOtpExpiredError();
      expect(err.statusCode).toBe(400);
      expect(err.code).toBe('AUTH_OTP_EXPIRED');
    });

    it('AuthOtpInvalidError should be 400', () => {
      const err = new AuthOtpInvalidError();
      expect(err.statusCode).toBe(400);
      expect(err.code).toBe('AUTH_OTP_INVALID');
    });

    it('AuthOtpLimitError should be 429', () => {
      const err = new AuthOtpLimitError();
      expect(err.statusCode).toBe(429);
      expect(err.code).toBe('AUTH_OTP_LIMIT');
    });

    it('AuthTokenExpiredError should be 401', () => {
      const err = new AuthTokenExpiredError();
      expect(err.statusCode).toBe(401);
      expect(err.code).toBe('AUTH_TOKEN_EXPIRED');
    });

    it('AuthTokenInvalidError should be 401', () => {
      const err = new AuthTokenInvalidError();
      expect(err.statusCode).toBe(401);
      expect(err.code).toBe('AUTH_TOKEN_INVALID');
    });

    it('UserBlockedError should be 403', () => {
      const err = new UserBlockedError();
      expect(err.statusCode).toBe(403);
      expect(err.code).toBe('USER_BLOCKED');
    });
  });

  describe('Other Errors', () => {
    it('ValidationError should be 400 with details', () => {
      const err = new ValidationError('Invalid input', [{ field: 'phone' }]);
      expect(err.statusCode).toBe(400);
      expect(err.code).toBe('VALIDATION_ERROR');
      expect(err.details).toEqual([{ field: 'phone' }]);
    });

    it('NotFoundError should be 404', () => {
      const err = new NotFoundError();
      expect(err.statusCode).toBe(404);
      expect(err.code).toBe('NOT_FOUND');
    });

    it('RateLimitError should be 429', () => {
      const err = new RateLimitError();
      expect(err.statusCode).toBe(429);
      expect(err.code).toBe('RATE_LIMIT_EXCEEDED');
    });
  });
});
