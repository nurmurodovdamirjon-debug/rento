import { Pool } from 'pg';
import { logger } from '../utils/logger';
import { config } from './index';

export const pool = new Pool({
  host: config.db.host,
  port: config.db.port,
  database: config.db.database,
  user: config.db.user,
  password: config.db.password,
  max: config.db.max,
});

pool.on('connect', () => {
  logger.debug('PostgreSQL pool: new client connected');
});

pool.on('error', (err) => {
  logger.error('PostgreSQL pool error:', err);
});

export async function connectDatabase(): Promise<void> {
  try {
    const client = await pool.connect();
    const result = await client.query('SELECT NOW()');
    client.release();
    logger.info(`PostgreSQL connected: ${result.rows[0].now}`);
  } catch (error) {
    logger.error('Failed to connect to PostgreSQL:', error);
    throw error;
  }
}

export async function closeDatabase(): Promise<void> {
  await pool.end();
  logger.info('PostgreSQL pool closed');
}
