import { Router } from 'express';
import { logoutHandler, refreshTokenHandler, sendOtp, verifyOtpHandler } from '../controllers/auth.controller';
import { authMiddleware } from '../middleware/auth.middleware';
import { otpVerifyRateLimiter, smsRateLimiter } from '../middleware/rateLimiter.middleware';

const router = Router();

// POST /api/v1/auth/send-otp — SMS OTP yuborish
router.post('/send-otp', smsRateLimiter, sendOtp);

// POST /api/v1/auth/verify-otp — OTP tasdiqlash
router.post('/verify-otp', otpVerifyRateLimiter, verifyOtpHandler);

// POST /api/v1/auth/refresh-token — Token yangilash
router.post('/refresh-token', refreshTokenHandler);

// POST /api/v1/auth/logout — Chiqish (auth talab qilinadi)
router.post('/logout', authMiddleware, logoutHandler);

export default router;
