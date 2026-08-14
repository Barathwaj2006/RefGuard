import express from 'express';
import apiRoutes from './routes/api';
import { errorHandler } from './middleware/errorHandler';

const app = express();

app.use(express.json());

// Mount the API v1 routes
app.use('/api/v1', apiRoutes);

// Global Error Handler
app.use(errorHandler);

export default app;
