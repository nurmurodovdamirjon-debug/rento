import { NextFunction, Request, Response } from 'express';
import { logger } from '../utils/logger';

/**
 * HTTP Request Logger Middleware
 * Har bir request uchun method, path, status, latency log qiladi
 */
export function requestLogger(req: Request, res: Response, next: NextFunction): void {
  const start = Date.now();
  const { method, originalUrl, ip } = req;

  res.on('finish', () => {
    const latency = Date.now() - start;
    const { statusCode } = res;

    const logData = {
      method,
      path: originalUrl,
      status: statusCode,
      latency: `${latency}ms`,
      ip,
      userAgent: req.get('user-agent') || '-',
      contentLength: res.get('content-length') || '0',
    };

    if (statusCode >= 500) {
      logger.error('request', logData);
    } else if (statusCode >= 400) {
      logger.warn('request', logData);
    } else {
      logger.info('request', logData);
    }
  });

  next();
}
