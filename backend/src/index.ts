import app from './app';

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`RefGuard Backend API listening on port ${PORT}`);
  console.log('  - API Endpoint (POST): http://localhost:' + PORT + '/api/v1/scan');
  console.log('  - Report Endpoint (POST): http://localhost:' + PORT + '/api/v1/report');
  console.log('  - Interactive Demo UI: http://localhost:' + PORT + '/');
});
