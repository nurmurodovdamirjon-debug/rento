import { NextFunction, Response } from 'express';
import { verifyAccessToken } from '../services/token.service';
import { AuthRequest } from '../types';
import { AuthTokenExpiredError, AuthTokenInvalidError } from '../utils/errors';
import { logger } from '../utils/logger';

/**
 * JWT auth middleware
 * Authorization: Bearer <token> headerdan token oladi
 * Token tasdiqlanganda req.user ga payload yozadi
 */
export function authMiddleware(req: AuthRequest, _res: Response, next: NextFunction): void {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      throw new AuthTokenInvalidError('Authorization header topilmadi');
    }

    const token = authHeader.split(' ')[1];

    if (!token) {
      throw new AuthTokenInvalidError('Token bo\'sh');
    }

    const decoded = verifyAccessToken(token);

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

    // JWT library errors
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
