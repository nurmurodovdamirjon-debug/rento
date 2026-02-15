import { NextFunction, Response } from 'express';
import jwt from 'jsonwebtoken';
import { Socket } from 'socket.io';
import { config } from '../config';
import { AuthRequest, AuthSocket, JwtPayload } from '../types';
import { AuthTokenExpiredError, AuthTokenInvalidError } from '../utils/errors';
import { logger } from '../utils/logger';

/**
 * HTTP so'rovlar uchun JWT auth middleware.
 */
export function authMiddleware(req: AuthRequest, _res: Response, next: NextFunction): void {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new AuthTokenInvalidError('Authorization header topilmadi');
    }

    const token = authHeader.split(' ')[1];
    if (!token) {
      throw new AuthTokenInvalidError("Token bo'sh");
    }

    const decoded = jwt.verify(token, config.jwt.accessSecret) as JwtPayload;

    req.user = {
      id: decoded.sub,
      role: decoded.role,
      phone: decoded.phone,
    };

    next();
  } catch (error: unknown) {
    if (error instanceof AuthTokenExpiredError || error instanceof AuthTokenInvalidError) {
      next(error);
      return;
    }
    const err = error as Error;
    if (err.name === 'TokenExpiredError') {
      next(new AuthTokenExpiredError());
      return;
    }
    if (err.name === 'JsonWebTokenError' || err.name === 'NotBeforeError') {
      next(new AuthTokenInvalidError());
      return;
    }
    logger.error('Auth middleware error:', error);
    next(new AuthTokenInvalidError());
  }
}

/**
 * Socket.IO ulanish uchun JWT auth middleware.
 * Token query parametr orqali yuboriladi: ?token=<access_token>
 */
export function socketAuthMiddleware(socket: Socket, next: (err?: Error) => void): void {
  try {
    const token = socket.handshake.query.token as string;
    if (!token) {
      next(new AuthTokenInvalidError('Token talab qilinadi'));
      return;
    }

    const decoded = jwt.verify(token, config.jwt.accessSecret) as JwtPayload;

    (socket as AuthSocket).data = {
      userId: decoded.sub,
      role: decoded.role,
      phone: decoded.phone,
    };

    next();
  } catch (error: unknown) {
    const err = error as Error;
    if (err.name === 'TokenExpiredError') {
      next(new AuthTokenExpiredError());
      return;
    }
    logger.error('Socket auth error:', error);
    next(new AuthTokenInvalidError());
  }
}
