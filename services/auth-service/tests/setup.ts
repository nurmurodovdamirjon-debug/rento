// Jest uchun test environment o'zgaruvchilari
process.env.JWT_ACCESS_SECRET = 'test-access-secret-32-chars-long!';
process.env.JWT_REFRESH_SECRET = 'test-refresh-secret-32-chars-lon!';
process.env.JWT_ACCESS_EXPIRY_SECONDS = '900';
process.env.JWT_REFRESH_EXPIRY_SECONDS = '604800';
process.env.APP_ENV = 'test';
process.env.DB_HOST = 'localhost';
process.env.DB_PORT = '5432';
process.env.DB_NAME = 'rento_test';
process.env.DB_USER = 'rento_user';
process.env.DB_PASSWORD = 'rento_secret_password';
process.env.REDIS_HOST = 'localhost';
process.env.REDIS_PORT = '6379';
process.env.AUTH_SERVICE_PORT = '3001';
