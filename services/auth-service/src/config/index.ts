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
  },

  corsOrigins: (process.env.CORS_ALLOWED_ORIGINS || '').split(','),
};
