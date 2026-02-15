import jwt, { SignOptions } from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';
import { config } from '../config';
import { AccessTokenPayload, RefreshTokenPayload } from '../types';

export function generateAccessToken(payload: { sub: string; role: string; phone: string }): string {
  const tokenPayload: AccessTokenPayload = {
    sub: payload.sub,
    role: payload.role,
    phone: payload.phone,
    iss: 'rento.uz',
  };

  const options: SignOptions = {
    expiresIn: config.jwt.accessExpirySeconds,
  };

  return jwt.sign(tokenPayload as object, config.jwt.accessSecret, options);
}

export function generateRefreshToken(payload: { sub: string; role: string; phone: string }): string {
  const tokenPayload: RefreshTokenPayload = {
    sub: payload.sub,
    role: payload.role,
    phone: payload.phone,
    iss: 'rento.uz',
    jti: uuidv4(),
    type: 'refresh',
  };

  const options: SignOptions = {
    expiresIn: config.jwt.refreshExpirySeconds,
  };

  return jwt.sign(tokenPayload as object, config.jwt.refreshSecret, options);
}

export function generateTokenPair(user: { id: string; role: string; phone: string }): {
  accessToken: string;
  refreshToken: string;
} {
  const payload = { sub: user.id, role: user.role, phone: user.phone };
  return {
    accessToken: generateAccessToken(payload),
    refreshToken: generateRefreshToken(payload),
  };
}

export function verifyAccessToken(token: string): AccessTokenPayload {
  return jwt.verify(token, config.jwt.accessSecret) as AccessTokenPayload;
}

export function verifyRefreshToken(token: string): RefreshTokenPayload {
  return jwt.verify(token, config.jwt.refreshSecret) as RefreshTokenPayload;
}
