import { z } from 'zod';

// Telefon raqam: +998XXXXXXXXX (13 belgi)
export const sendOtpSchema = z.object({
  phone: z
    .string()
    .regex(/^\+998\d{9}$/, 'Telefon raqam formati: +998XXXXXXXXX')
    .length(13, 'Telefon raqam 13 belgi bo\'lishi kerak'),
});

// OTP tasdiqlash: phone + 6 xonali otp
export const verifyOtpSchema = z.object({
  phone: z
    .string()
    .regex(/^\+998\d{9}$/, 'Telefon raqam formati: +998XXXXXXXXX')
    .length(13, 'Telefon raqam 13 belgi bo\'lishi kerak'),
  otp: z
    .string()
    .regex(/^\d{6}$/, 'OTP 6 ta raqamdan iborat bo\'lishi kerak')
    .length(6, 'OTP 6 ta raqamdan iborat bo\'lishi kerak'),
});

// Refresh token
export const refreshTokenSchema = z.object({
  refresh_token: z
    .string()
    .min(1, 'Refresh token talab qilinadi'),
});

// Validatsiya helper
export function validate<T>(schema: z.ZodSchema<T>, data: unknown): T {
  return schema.parse(data);
}
