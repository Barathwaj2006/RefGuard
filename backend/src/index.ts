import app from './app';
import { logger } from './utils/logger';

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  logger.info('RefGuard Backend API started', { 
    port: PORT, 
    environment: process.env.NODE_ENV || 'development',
    endpoints: {
      scan: `http://localhost:${PORT}/api/v1/scan`,
      report: `http://localhost:${PORT}/api/v1/report`,
      health: `http://localhost:${PORT}/health`,
      demo: `http://localhost:${PORT}/`
    }
  });
  console.log(`RefGuard Backend API listening on port ${PORT}`);
  console.log('  - API Endpoint (POST): http://localhost:' + PORT + '/api/v1/scan');
  console.log('  - Report Endpoint (POST): http://localhost:' + PORT + '/api/v1/report');
  console.log('  - Interactive Demo UI: http://localhost:' + PORT + '/');
});
