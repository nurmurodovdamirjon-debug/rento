export const config = {
  port: parseInt(process.env.AUTH_SERVICE_PORT || '3001'),
  appEnv: process.env.APP_ENV || 'development',
  appVersion: process.env.APP_VERSION || '0.1.0',

  db: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432'),
    database: process.env.DB_NAME || 'rento',
    user: process.env.DB_USER || 'rento_user',
    password: process.env.DB_PASSWORD || 'rento_secret_password',
    max: parseInt(process.env.DB_MAX_CONNECTIONS || '25'),
  },

  redis: {
    host: process.env.REDIS_HOST || 'localhost',
    port: parseInt(process.env.REDIS_PORT || '6379'),
    password: process.env.REDIS_PASSWORD || undefined,
  },

  jwt: {
    accessSecret: process.env.JWT_ACCESS_SECRET || '',
    refreshSecret: process.env.JWT_REFRESH_SECRET || '',
    accessExpiry: process.env.JWT_ACCESS_EXPIRY || '15m',
    refreshExpiry: process.env.JWT_REFRESH_EXPIRY || '7d',
    accessExpirySeconds: parseInt(process.env.JWT_ACCESS_EXPIRY_SECONDS || '900'),
    refreshExpirySeconds: parseInt(process.env.JWT_REFRESH_EXPIRY_SECONDS || '604800'),
  },

  sms: {
    provider: process.env.SMS_PROVIDER || 'eskiz',
    eskizEmail: process.env.ESKIZ_EMAIL || '',
    eskizPassword: process.env.ESKIZ_PASSWORD || '',
    eskizBaseUrl: process.env.ESKIZ_BASE_URL || 'https://notify.eskiz.uz/api',
  },

  rateLimit: {
    smsMaxPerHour: parseInt(process.env.RATE_LIMIT_SMS_MAX || '3'),
    smsWindowSeconds: parseInt(process.env.RATE_LIMIT_SMS_WINDOW || '3600'),
    otpVerifyMax: 5,
    otpVerifyWindowSeconds: 300,
    apiMaxPerMinute: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS || '60'),
  },

  otp: {
    length: 6,
    expirySeconds: 300,
  },

  corsOrigins: (process.env.CORS_ALLOWED_ORIGINS || '').split(','),
};
