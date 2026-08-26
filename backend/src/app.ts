import express, { Request, Response } from 'express';
import apiRoutes from './routes/api';
import { errorHandler } from './middleware/errorHandler';
import { logger } from './utils/logger';

const app = express();

// Validate required environment variables at startup (skip in test environment)
if (process.env.NODE_ENV !== 'test') {
  const requiredEnvVars = ['GEMINI_API_KEY'];
  const missingEnvVars = requiredEnvVars.filter(envVar => !process.env[envVar]);

  if (missingEnvVars.length > 0) {
    logger.error('Missing required environment variables', undefined, { missing: missingEnvVars });
    console.error(`ERROR: Missing required environment variables: ${missingEnvVars.join(', ')}`);
    console.error('Please set these variables before starting the server.');
    process.exit(1);
  }

  logger.info('Environment validation passed', { checked: requiredEnvVars });
}

// CORS configuration - restrict origins in production
const ALLOWED_ORIGINS = process.env.ALLOWED_ORIGINS?.split(',') || ['http://localhost:3000', 'http://localhost:8080'];

app.use((req, res, next) => {
  const origin = req.headers.origin;
  
  // Check if origin is allowed
  if (origin && ALLOWED_ORIGINS.includes(origin)) {
    res.setHeader('Access-Control-Allow-Origin', origin);
    logger.debug('CORS origin allowed', { origin });
  } else if (process.env.NODE_ENV === 'development') {
    // Allow all origins in development only
    res.setHeader('Access-Control-Allow-Origin', '*');
  } else if (origin) {
    // Log rejected origins in production for monitoring
    logger.warn('CORS origin rejected', { origin, allowedOrigins: ALLOWED_ORIGINS });
  }
  // In production, if origin doesn't match, no CORS header is set (browser will block)
  
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-api-key');
  res.setHeader('Access-Control-Expose-Headers', 'X-Gemini-Used');
  if (req.method === 'OPTIONS') {
    return res.sendStatus(204);
  }
  next();
});

app.use(express.json());

import path from 'path';
app.use(express.static(path.join(__dirname, '../../integration/demo/public')));

// Root Health Check
app.get('/health', (_req: Request, res: Response) => {
  res.status(200).json({ status: 'UP', service: 'RefGuard' });
});

// Mount the API v1 routes
app.use('/api/v1', apiRoutes);

// Global Error Handler
app.use(errorHandler);

export default app;

