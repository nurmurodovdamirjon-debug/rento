import Redis from 'ioredis';
import { logger } from '../utils/logger';
import { config } from './index';

export const redis = new Redis({
  host: config.redis.host,
  port: config.redis.port,
  password: config.redis.password,
  maxRetriesPerRequest: 3,
  retryStrategy(times: number) {
    const delay = Math.min(times * 200, 3000);
    return delay;
  },
});

redis.on('connect', () => {
  logger.info('Redis connected');
});

redis.on('error', (err) => {
  logger.error('Redis connection error:', err);
});

redis.on('close', () => {
  logger.warn('Redis connection closed');
});

export async function closeRedis(): Promise<void> {
  await redis.quit();
  logger.info('Redis connection closed gracefully');
}
