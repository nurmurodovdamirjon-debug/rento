import cors from 'cors';
import express, { NextFunction, Request, Response } from 'express';
import helmet from 'helmet';
import http from 'http';
import { Server } from 'socket.io';
import { config } from './config';
import { closeDatabase, connectDatabase } from './config/database';
import { closeRedis, redis } from './config/redis';
import { registerSocketHandlers } from './handlers/socket.handler';
import { socketAuthMiddleware } from './middleware/auth.middleware';
import chatRoutes from './routes/chat.routes';
import { AppError } from './utils/errors';
import { logger } from './utils/logger';

const app = express();
const httpServer = http.createServer(app);

// Socket.IO server
const io = new Server(httpServer, {
  cors: {
    origin: config.corsOrigins,
    credentials: true,
  },
  path: '/ws/chat',
  pingInterval: 25000,
  pingTimeout: 10000,
});

// Middleware
app.use(helmet());
app.use(cors({ origin: config.corsOrigins, credentials: true }));
app.use(express.json({ limit: '10mb' }));

// Health check
app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    service: 'chat-service',
    version: config.appVersion,
  });
});

// REST Routes
app.use('/api/v1/chats', chatRoutes);

// 404 handler
app.use((_req: Request, res: Response) => {
  res.status(404).json({
    success: false,
    error: { code: 'NOT_FOUND', message: 'Endpoint topilmadi' },
  });
});

// Error handler
app.use((err: Error, _req: Request, res: Response, _next: NextFunction) => {
  if (err instanceof AppError) {
    res.status(err.statusCode).json({
      success: false,
      error: { code: err.code, message: err.message, details: err.details },
    });
    return;
  }
  logger.error('Unhandled error:', err);
  res.status(500).json({
    success: false,
    error: { code: 'INTERNAL_ERROR', message: 'Ichki server xatosi' },
  });
});

// Socket.IO auth middleware
io.use(socketAuthMiddleware);

// WebSocket handlers
registerSocketHandlers(io);

// Start
async function start(): Promise<void> {
  try {
    await connectDatabase();
    logger.info('PostgreSQL connected');

    await redis.ping();
    logger.info('Redis connected');

    httpServer.listen(config.port, () => {
      logger.info(
        `Chat service running on port ${config.port} (env: ${config.appEnv}) — REST + WebSocket`
      );
    });

    const shutdown = async (signal: string) => {
      logger.info(`${signal} received. Shutting down gracefully...`);
      io.close();
      httpServer.close(async () => {
        await closeDatabase();
        await closeRedis();
        logger.info('Chat service stopped');
        process.exit(0);
      });
      setTimeout(() => {
        logger.error('Forced shutdown after timeout');
        process.exit(1);
      }, 10000);
    };

    process.on('SIGTERM', () => shutdown('SIGTERM'));
    process.on('SIGINT', () => shutdown('SIGINT'));
  } catch (error) {
    logger.error('Failed to start chat service:', error);
    process.exit(1);
  }
}

start();

export { io };
export default app;
