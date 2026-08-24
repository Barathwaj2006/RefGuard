import express, { Request, Response } from 'express';
import apiRoutes from './routes/api';
import { errorHandler } from './middleware/errorHandler';

const app = express();

// Enable CORS for frontend and demo dashboard access
app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-api-key');
  res.setHeader('Access-Control-Expose-Headers', 'X-Gemini-Used');
  if (req.method === 'OPTIONS') {
    return res.sendStatus(204);
  }
  next();
});

app.use(express.json());

// Root Health Check
app.get('/health', (_req: Request, res: Response) => {
  res.status(200).json({ status: 'UP', service: 'RefGuard' });
});

// Mount the API v1 routes
app.use('/api/v1', apiRoutes);

// Global Error Handler
app.use(errorHandler);

export default app;

