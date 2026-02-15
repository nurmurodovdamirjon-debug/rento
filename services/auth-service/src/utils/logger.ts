import path from 'path';
import winston from 'winston';
import { config } from '../config';

const logDir = path.join(process.cwd(), 'logs');

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
    // Combined log — barcha loglar
    new winston.transports.File({
      filename: path.join(logDir, 'combined.log'),
      maxsize: 10 * 1024 * 1024, // 10 MB
      maxFiles: 5,
      tailable: true,
    }),
    // Error log — faqat xatoliklar
    new winston.transports.File({
      filename: path.join(logDir, 'error.log'),
      level: 'error',
      maxsize: 10 * 1024 * 1024, // 10 MB
      maxFiles: 10,
      tailable: true,
    }),
  ],
});
