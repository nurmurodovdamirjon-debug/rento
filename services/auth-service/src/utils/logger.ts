import winston from 'winston';
import { config } from '../config';

export const logger = winston.createLogger({
  level: config.appEnv === 'development' ? 'debug' : 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.errors({ stack: true }),
    config.appEnv === 'development'
      ? winston.format.simple()
      : winston.format.json()
  ),
  defaultMeta: { service: 'auth-service' },
  transports: [
    new winston.transports.Console(),
  ],
});
