import { config } from '../config';
import { pool } from '../config/database';
import { redis } from '../config/redis';
import { RefreshTokenResponse, SendOtpResponse, User, VerifyOtpResponse } from '../types';
import {
    AuthOtpExpiredError,
    AuthOtpInvalidError,
    AuthOtpLimitError,
    AuthTokenInvalidError,
    UserBlockedError,
} from '../utils/errors';
import { logger } from '../utils/logger';
import { generateOtp, sendOtp as sendSms } from './sms.service';
import { generateTokenPair, verifyRefreshToken } from './token.service';

// Redis key patterns
const OTP_KEY = (phone: string) => `auth:otp:${phone}`;
const OTP_ATTEMPTS_KEY = (phone: string) => `auth:otp_attempts:${phone}`;
const SESSION_KEY = (userId: string) => `auth:session:${userId}`;

/**
 * OTP yuborish
 * 1. Rate limit tekshirish (3 ta / soat)
 * 2. OTP generatsiya qilish
 * 3. Redis ga saqlash (300 soniya TTL)
 * 4. SMS yuborish
 */
export async function sendOtpToPhone(phone: string): Promise<SendOtpResponse> {
  // Rate limit — soatiga 3 ta SMS
  const attemptsKey = OTP_ATTEMPTS_KEY(phone);
  const currentAttempts = await redis.get(attemptsKey);
  const attempts = currentAttempts ? parseInt(currentAttempts) : 0;

  if (attempts >= config.rateLimit.smsMaxPerHour) {
    throw new AuthOtpLimitError();
  }

  // OTP generatsiya va Redis ga saqlash
  const otp = generateOtp();
  const otpKey = OTP_KEY(phone);

  await redis.setex(otpKey, config.otp.expirySeconds, otp);

  // Attempts counter — agar birinchi marta bo'lsa, TTL o'rnatamiz
  await redis.incr(attemptsKey);
  if (attempts === 0) {
    await redis.expire(attemptsKey, config.rateLimit.smsWindowSeconds);
  }

  // SMS yuborish
  await sendSms(phone, otp);

  const attemptsRemaining = config.rateLimit.smsMaxPerHour - attempts - 1;

  logger.info(`OTP sent to ${phone.slice(0, 7)}****, attempts remaining: ${attemptsRemaining}`);

  return {
    phone,
    expires_in: config.otp.expirySeconds,
    retry_after: 60,
    attempts_remaining: attemptsRemaining,
  };
}

/**
 * OTP tasdiqlash
 * 1. Redis dan OTP olish
 * 2. Tekshirish
 * 3. User topish yoki yaratish
 * 4. JWT pair generatsiya qilish
 * 5. Session Redis ga saqlash
 */
export async function verifyOtp(phone: string, otp: string): Promise<VerifyOtpResponse> {
  const otpKey = OTP_KEY(phone);
  const storedOtp = await redis.get(otpKey);

  logger.info(`[VERIFY] phone="${phone}", otpKey="${otpKey}", storedOtp="${storedOtp}", receivedOtp="${otp}"`);

  if (!storedOtp) {
    logger.warn(`[VERIFY FAIL] OTP expired for phone="${phone}"`);
    throw new AuthOtpExpiredError();
  }

  if (storedOtp !== otp) {
    logger.warn(`[VERIFY FAIL] OTP invalid for phone="${phone}", expected="${storedOtp}", got="${otp}"`);
    throw new AuthOtpInvalidError();
  }

  // OTP dan foydalanildi — o'chirish
  await redis.del(otpKey);

  // Foydalanuvchini topish yoki yaratish
  const { user, isNewUser } = await findOrCreateUser(phone);

  // Bloklangan tekshirish
  if (user.is_blocked) {
    throw new UserBlockedError();
  }

  // JWT pair generatsiya
  const tokens = generateTokenPair({
    id: user.id,
    role: user.role,
    phone: user.phone,
  });

  // Refresh token ni Redis ga saqlash (7 kun)
  await redis.setex(
    SESSION_KEY(user.id),
    config.jwt.refreshExpirySeconds,
    tokens.refreshToken
  );

  // last_seen_at yangilash
  await pool.query(
    'UPDATE users SET last_seen_at = NOW() WHERE id = $1',
    [user.id]
  );

  logger.info(`User authenticated: ${user.id} (${isNewUser ? 'new' : 'existing'})`);

  return {
    access_token: tokens.accessToken,
    refresh_token: tokens.refreshToken,
    token_type: 'Bearer',
    expires_in: config.jwt.accessExpirySeconds,
    user: {
      id: user.id,
      phone: user.phone,
      full_name: user.full_name,
      role: user.role,
      is_new_user: isNewUser,
    },
  };
}

/**
 * Token yangilash (rotation)
 * 1. Refresh token verify
 * 2. Redis dan session tekshirish
 * 3. Yangi token pair generatsiya
 * 4. Eski session o'chirish, yangi yozish
 */
export async function refreshToken(token: string): Promise<RefreshTokenResponse> {
  let decoded;
  try {
    decoded = verifyRefreshToken(token);
  } catch {
    throw new AuthTokenInvalidError('Refresh token noto\'g\'ri yoki muddati tugagan');
  }

  // Redis dan session tekshirish
  const sessionKey = SESSION_KEY(decoded.sub);
  const storedToken = await redis.get(sessionKey);

  if (!storedToken || storedToken !== token) {
    throw new AuthTokenInvalidError('Session topilmadi yoki token mos emas');
  }

  // Yangi token pair generatsiya (rotation)
  const tokens = generateTokenPair({
    id: decoded.sub,
    role: decoded.role,
    phone: decoded.phone,
  });

  // Session yangilash
  await redis.setex(
    sessionKey,
    config.jwt.refreshExpirySeconds,
    tokens.refreshToken
  );

  logger.info(`Token refreshed for user: ${decoded.sub}`);

  return {
    access_token: tokens.accessToken,
    refresh_token: tokens.refreshToken,
    token_type: 'Bearer',
    expires_in: config.jwt.accessExpirySeconds,
  };
}

/**
 * Chiqish (logout)
 * Redis dan session o'chirish
 */
export async function logout(userId: string): Promise<void> {
  await redis.del(SESSION_KEY(userId));
  logger.info(`User logged out: ${userId}`);
}

/**
 * Foydalanuvchini topish yoki yaratish
 */
async function findOrCreateUser(phone: string): Promise<{ user: User; isNewUser: boolean }> {
  // Mavjud foydalanuvchini qidirish
  const existing = await pool.query<User>(
    'SELECT * FROM users WHERE phone = $1',
    [phone]
  );

  if (existing.rows.length > 0) {
    return { user: existing.rows[0], isNewUser: false };
  }

  // Yangi foydalanuvchi yaratish
  const newUser = await pool.query<User>(
    `INSERT INTO users (phone, phone_verified, role, language)
     VALUES ($1, true, 'tenant', 'uz')
     RETURNING *`,
    [phone]
  );

  logger.info(`New user created: ${newUser.rows[0].id}, phone: ${phone.slice(0, 7)}****`);

  return { user: newUser.rows[0], isNewUser: true };
}
