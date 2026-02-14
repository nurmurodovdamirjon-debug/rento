import cors from 'cors';
import express from 'express';
import helmet from 'helmet';
import { config } from './config';
import { logger } from './utils/logger';

const app = express();

// Middleware
app.use(helmet());
app.use(cors({ origin: config.corsOrigins }));
app.use(express.json({ limit: '10mb' }));

// Health check
app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    service: 'auth-service',
    version: config.appVersion,
  });
});

// Routes (Sprint 1 da qo'shiladi)
// app.use('/api/v1/auth', authRoutes);

// Start
app.listen(config.port, () => {
  logger.info(`Auth service running on port ${config.port}`);
});

export default app;
