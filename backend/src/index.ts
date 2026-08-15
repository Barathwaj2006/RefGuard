import app from './app';

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`RefGuard Backend API listening on port ${PORT}`);
});
