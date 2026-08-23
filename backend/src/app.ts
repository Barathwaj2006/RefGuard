import express, { Request, Response } from 'express';
import apiRoutes from './routes/api';
import { errorHandler } from './middleware/errorHandler';

const app = express();

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
