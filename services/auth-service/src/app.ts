import cors from 'cors';
import express, { NextFunction, Request, Response } from 'express';
import helmet from 'helmet';
import { config } from './config';
import { closeDatabase, connectDatabase } from './config/database';
import { closeRedis, redis } from './config/redis';
import authRoutes from './routes/auth.routes';
import { AppError } from './utils/errors';
import { logger } from './utils/logger';

const app = express();

// Middleware
app.use(helmet());
app.use(cors({ origin: config.corsOrigins, credentials: true }));
app.use(express.json({ limit: '10mb' }));

// Health check
app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    service: 'auth-service',
    version: config.appVersion,
  });
});

// Auth routes
app.use('/api/v1/auth', authRoutes);

// 404 handler
app.use((_req: Request, res: Response) => {
  res.status(404).json({
    success: false,
    error: {
      code: 'NOT_FOUND',
      message: 'Endpoint topilmadi',
    },
  });
});

// Global error handler
app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  if (err instanceof AppError) {
    res.status(err.statusCode).json({
      success: false,
      error: {
        code: err.code,
        message: err.message,
        details: err.details,
      },
    });
    return;
  }

  // Kutilmagan xato
  logger.error('Unhandled error:', err);
  res.status(500).json({
    success: false,
    error: {
      code: 'INTERNAL_ERROR',
      message: 'Ichki server xatosi',
    },
  });
});

// Start server
async function start(): Promise<void> {
  try {
    // Database va Redis ulanish
    await connectDatabase();
    logger.info('PostgreSQL connected');

    await redis.ping();
    logger.info('Redis connected');

    const server = app.listen(config.port, () => {
      logger.info(`Auth service running on port ${config.port} (env: ${config.appEnv})`);
    });

    // Graceful shutdown
    const shutdown = async (signal: string) => {
      logger.info(`${signal} received. Shutting down gracefully...`);
      server.close(async () => {
        await closeDatabase();
        await closeRedis();
        logger.info('Auth service stopped');
        process.exit(0);
      });

      // Force close after 10 seconds
      setTimeout(() => {
        logger.error('Forced shutdown after timeout');
        process.exit(1);
      }, 10000);
    };

    process.on('SIGTERM', () => shutdown('SIGTERM'));
    process.on('SIGINT', () => shutdown('SIGINT'));
  } catch (error) {
    logger.error('Failed to start auth service:', error);
    process.exit(1);
  }
}

start();

export default app;
