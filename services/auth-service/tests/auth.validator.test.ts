import { ZodError } from 'zod';
import { refreshTokenSchema, sendOtpSchema, validate, verifyOtpSchema } from '../src/validators/auth.validator';

describe('Auth Validators', () => {
  describe('sendOtpSchema', () => {
    it('should accept valid Uzbek phone number', () => {
      const result = sendOtpSchema.parse({ phone: '+998901234567' });
      expect(result.phone).toBe('+998901234567');
    });

    it('should reject phone without +998 prefix', () => {
      expect(() => sendOtpSchema.parse({ phone: '+1234567890' })).toThrow(ZodError);
    });

    it('should reject phone with wrong length', () => {
      expect(() => sendOtpSchema.parse({ phone: '+99890123456' })).toThrow(ZodError);
      expect(() => sendOtpSchema.parse({ phone: '+9989012345678' })).toThrow(ZodError);
    });

    it('should reject phone with letters', () => {
      expect(() => sendOtpSchema.parse({ phone: '+998abcdefgh' })).toThrow(ZodError);
    });

    it('should reject empty phone', () => {
      expect(() => sendOtpSchema.parse({ phone: '' })).toThrow(ZodError);
    });

    it('should reject missing phone', () => {
      expect(() => sendOtpSchema.parse({})).toThrow(ZodError);
    });
  });

  describe('verifyOtpSchema', () => {
    it('should accept valid phone and otp', () => {
      const result = verifyOtpSchema.parse({ phone: '+998901234567', otp: '123456' });
      expect(result.phone).toBe('+998901234567');
      expect(result.otp).toBe('123456');
    });

    it('should reject otp with letters', () => {
      expect(() => verifyOtpSchema.parse({ phone: '+998901234567', otp: 'abcdef' })).toThrow(ZodError);
    });

    it('should reject otp with wrong length', () => {
      expect(() => verifyOtpSchema.parse({ phone: '+998901234567', otp: '12345' })).toThrow(ZodError);
      expect(() => verifyOtpSchema.parse({ phone: '+998901234567', otp: '1234567' })).toThrow(ZodError);
    });

    it('should reject missing otp', () => {
      expect(() => verifyOtpSchema.parse({ phone: '+998901234567' })).toThrow(ZodError);
    });
  });

  describe('refreshTokenSchema', () => {
    it('should accept valid refresh token', () => {
      const result = refreshTokenSchema.parse({ refresh_token: 'some.jwt.token' });
      expect(result.refresh_token).toBe('some.jwt.token');
    });

    it('should reject empty refresh token', () => {
      expect(() => refreshTokenSchema.parse({ refresh_token: '' })).toThrow(ZodError);
    });

    it('should reject missing refresh_token', () => {
      expect(() => refreshTokenSchema.parse({})).toThrow(ZodError);
    });
  });

  describe('validate helper', () => {
    it('should return parsed data on valid input', () => {
      const result = validate(sendOtpSchema, { phone: '+998901234567' });
      expect(result.phone).toBe('+998901234567');
    });

    it('should throw ZodError on invalid input', () => {
      expect(() => validate(sendOtpSchema, { phone: 'invalid' })).toThrow(ZodError);
    });
  });
});
