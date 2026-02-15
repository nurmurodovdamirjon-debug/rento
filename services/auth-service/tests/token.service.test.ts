import { generateAccessToken, generateRefreshToken, generateTokenPair, verifyAccessToken, verifyRefreshToken } from '../src/services/token.service';

describe('Token Service', () => {
  const testPayload = {
    sub: '550e8400-e29b-41d4-a716-446655440000',
    role: 'tenant',
    phone: '+998901234567',
  };

  describe('generateAccessToken', () => {
    it('should generate a valid JWT access token', () => {
      const token = generateAccessToken(testPayload);
      expect(token).toBeDefined();
      expect(typeof token).toBe('string');
      expect(token.split('.')).toHaveLength(3);
    });

    it('should include correct claims', () => {
      const token = generateAccessToken(testPayload);
      const decoded = verifyAccessToken(token);
      expect(decoded.sub).toBe(testPayload.sub);
      expect(decoded.role).toBe(testPayload.role);
      expect(decoded.phone).toBe(testPayload.phone);
      expect(decoded.iss).toBe('rento.uz');
    });
  });

  describe('generateRefreshToken', () => {
    it('should generate a valid JWT refresh token', () => {
      const token = generateRefreshToken(testPayload);
      expect(token).toBeDefined();
      expect(typeof token).toBe('string');
    });

    it('should include jti and type claims', () => {
      const token = generateRefreshToken(testPayload);
      const decoded = verifyRefreshToken(token);
      expect(decoded.jti).toBeDefined();
      expect(decoded.type).toBe('refresh');
      expect(decoded.sub).toBe(testPayload.sub);
    });
  });

  describe('generateTokenPair', () => {
    it('should generate both access and refresh tokens', () => {
      const user = { id: testPayload.sub, role: testPayload.role, phone: testPayload.phone };
      const pair = generateTokenPair(user);
      expect(pair.accessToken).toBeDefined();
      expect(pair.refreshToken).toBeDefined();
      expect(pair.accessToken).not.toBe(pair.refreshToken);
    });
  });

  describe('verifyAccessToken', () => {
    it('should verify a valid token', () => {
      const token = generateAccessToken(testPayload);
      const decoded = verifyAccessToken(token);
      expect(decoded.sub).toBe(testPayload.sub);
    });

    it('should throw on invalid token', () => {
      expect(() => verifyAccessToken('invalid.token.here')).toThrow();
    });
  });

  describe('verifyRefreshToken', () => {
    it('should verify a valid refresh token', () => {
      const token = generateRefreshToken(testPayload);
      const decoded = verifyRefreshToken(token);
      expect(decoded.sub).toBe(testPayload.sub);
      expect(decoded.type).toBe('refresh');
    });

    it('should throw on invalid token', () => {
      expect(() => verifyRefreshToken('invalid.token.here')).toThrow();
    });

    it('should not verify access token as refresh token', () => {
      const accessToken = generateAccessToken(testPayload);
      // Access token uses different secret, so should throw
      expect(() => verifyRefreshToken(accessToken)).toThrow();
    });
  });
});
