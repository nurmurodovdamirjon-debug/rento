import { NextFunction, Request, Response } from 'express'
import { ZodError } from 'zod'
import * as authService from '../services/auth.service'
import { ApiResponse, AuthRequest } from '../types'
import { ValidationError } from '../utils/errors'
import { logger } from '../utils/logger'
import { refreshTokenSchema, sendOtpSchema, verifyOtpSchema } from '../validators/auth.validator'

/**
 * POST /api/v1/auth/send-otp
 * SMS OTP yuborish
 */
export async function sendOtp(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const { phone } = sendOtpSchema.parse(req.body);

    const result = await authService.sendOtpToPhone(phone);

    const response: ApiResponse = {
      success: true,
      data: result,
    };

    res.status(200).json(response);
  } catch (error) {
    if (error instanceof ZodError) {
      next(new ValidationError('Validatsiya xatosi', error.errors));
      return;
    }
    next(error);
  }
}

/**
 * POST /api/v1/auth/verify-otp
 * OTP tasdiqlash va token olish
 */
export async function verifyOtpHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    logger.info(`[VERIFY-OTP] Raw body: ${JSON.stringify(req.body)}`);
    const { phone, otp } = verifyOtpSchema.parse(req.body);

    const result = await authService.verifyOtp(phone, otp);

    const response: ApiResponse = {
      success: true,
      data: result,
    };

    res.status(200).json(response);
  } catch (error) {
    if (error instanceof ZodError) {
      logger.warn(`[VERIFY-OTP] Zod validation failed: ${JSON.stringify(error.errors)}`);
      next(new ValidationError('Validatsiya xatosi', error.errors));
      return;
    }
    logger.error(`[VERIFY-OTP] Error: ${error instanceof Error ? error.message : error}`);
    next(error);
  }
}

/**
 * POST /api/v1/auth/refresh-token
 * Token yangilash
 */
export async function refreshTokenHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const { refresh_token } = refreshTokenSchema.parse(req.body);

    const result = await authService.refreshToken(refresh_token);

    const response: ApiResponse = {
      success: true,
      data: result,
    };

    res.status(200).json(response);
  } catch (error) {
    if (error instanceof ZodError) {
      next(new ValidationError('Validatsiya xatosi', error.errors));
      return;
    }
    next(error);
  }
}

/**
 * POST /api/v1/auth/logout
 * Foydalanuvchi chiqishi
 */
export async function logoutHandler(req: AuthRequest, res: Response, next: NextFunction): Promise<void> {
  try {
    if (!req.user) {
      res.status(401).json({
        success: false,
        error: { code: 'AUTH_TOKEN_INVALID', message: 'Avtorizatsiya talab qilinadi' },
      });
      return;
    }

    await authService.logout(req.user.id);

    const response: ApiResponse = {
      success: true,
      data: { message: 'Muvaffaqiyatli chiqildi' },
    };

    res.status(200).json(response);
  } catch (error) {
    logger.error('Logout error:', error);
    next(error);
  }
}
