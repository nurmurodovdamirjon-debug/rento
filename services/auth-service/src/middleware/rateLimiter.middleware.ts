import { NextFunction, Request, Response } from 'express';
import { config } from '../config';
import { redis } from '../config/redis';
import { AuthOtpLimitError, RateLimitError } from '../utils/errors';
import { logger } from '../utils/logger';

/**
 * Umumiy rate limiter factory
 */
function createRateLimiter(options: {
  keyPrefix: string;
  maxRequests: number;
  windowSeconds: number;
  keyExtractor: (req: Request) => string;
  errorFactory: () => Error;
}) {
  return async (req: Request, _res: Response, next: NextFunction): Promise<void> => {
    try {
      const key = `${options.keyPrefix}:${options.keyExtractor(req)}`;
      const current = await redis.incr(key);

      if (current === 1) {
        await redis.expire(key, options.windowSeconds);
      }

      if (current > options.maxRequests) {
        logger.warn(`Rate limit exceeded: ${key}, current: ${current}`);
        throw options.errorFactory();
      }

      next();
    } catch (error) {
      if (error instanceof RateLimitError || error instanceof AuthOtpLimitError) {
        next(error);
      } else {
        next(error);
      }
    }
  };
}

/**
 * SMS OTP yuborish uchun rate limiter
 * Soatiga 3 ta SMS (telefon raqam bo'yicha)
 */
export const smsRateLimiter = createRateLimiter({
  keyPrefix: 'rate:sms',
  maxRequests: config.rateLimit.smsMaxPerHour,
  windowSeconds: config.rateLimit.smsWindowSeconds,
  keyExtractor: (req: Request) => req.body?.phone || req.ip || 'unknown',
  errorFactory: () => new AuthOtpLimitError(),
});

/**
 * OTP verify uchun rate limiter
 * 5 daqiqada 5 ta urinish (telefon raqam bo'yicha)
 */
export const otpVerifyRateLimiter = createRateLimiter({
  keyPrefix: 'rate:otp_verify',
  maxRequests: config.rateLimit.otpVerifyMax,
  windowSeconds: config.rateLimit.otpVerifyWindowSeconds,
  keyExtractor: (req: Request) => req.body?.phone || req.ip || 'unknown',
  errorFactory: () => new RateLimitError('OTP tasdiqlash urinishlari limiti oshdi'),
});

/**
 * Umumiy API rate limiter
 * Daqiqasiga 60 ta so'rov (IP bo'yicha)
 */
export const apiRateLimiter = createRateLimiter({
  keyPrefix: 'rate:api',
  maxRequests: config.rateLimit.apiMaxPerMinute,
  windowSeconds: 60,
  keyExtractor: (req: Request) => req.ip || 'unknown',
  errorFactory: () => new RateLimitError(),
});
