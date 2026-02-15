import { config } from '../config';
import { logger } from '../utils/logger';

/**
 * 6 xonali tasodifiy OTP generatsiya qilish
 */
export function generateOtp(): string {
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  return otp;
}

/**
 * SMS orqali OTP yuborish
 * Development: console ga chiqaradi
 * Production: Eskiz.uz API orqali yuboradi
 */
export async function sendOtp(phone: string, otp: string): Promise<void> {
  if (config.appEnv === 'development' || config.appEnv === 'test') {
    // Dev rejimda — console ga chiqarish
    logger.info(`[DEV SMS] Phone: ${phone}, OTP: ${otp}`);
    console.log(`\n========================================`);
    console.log(`  SMS OTP: ${otp}`);
    console.log(`  Phone:   ${phone}`);
    console.log(`========================================\n`);
    return;
  }

  // Production — Eskiz.uz API
  try {
    const tokenResponse = await fetch(`${config.sms.eskizBaseUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: config.sms.eskizEmail,
        password: config.sms.eskizPassword,
      }),
    });

    if (!tokenResponse.ok) {
      throw new Error(`Eskiz auth failed: ${tokenResponse.status}`);
    }

    const tokenData = await tokenResponse.json() as { data: { token: string } };
    const eskizToken = tokenData.data.token;

    const smsResponse = await fetch(`${config.sms.eskizBaseUrl}/message/sms/send`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${eskizToken}`,
      },
      body: JSON.stringify({
        mobile_phone: phone.replace('+', ''),
        message: `Rento tasdiqlash kodi: ${otp}. 5 daqiqa ichida kiriting.`,
        from: '4546',
      }),
    });

    if (!smsResponse.ok) {
      throw new Error(`Eskiz SMS failed: ${smsResponse.status}`);
    }

    logger.info(`SMS sent to ${phone.slice(0, 7)}****`);
  } catch (error) {
    logger.error('SMS send failed:', error);
    throw error;
  }
}
