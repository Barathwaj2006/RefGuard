import { Request, Response, NextFunction } from 'express';

interface RequestBucket {
  count: number;
  resetTime: number;
}

const ipBuckets = new Map<string, RequestBucket>();
const WINDOW_MS = 60 * 1000; // 1 minute window
const MAX_REPORTS_PER_WINDOW = 30; // Max 30 reports per IP/minute

export const reportRateLimiter = (req: Request, res: Response, next: NextFunction): void => {
  const ip = req.ip || req.socket.remoteAddress || 'unknown_ip';
  const now = Date.now();

  let bucket = ipBuckets.get(ip);
  if (!bucket || now > bucket.resetTime) {
    bucket = { count: 1, resetTime: now + WINDOW_MS };
    ipBuckets.set(ip, bucket);
    return next();
  }

  bucket.count++;
  if (bucket.count > MAX_REPORTS_PER_WINDOW) {
    res.status(429).json({
      error_code: 'RATE_LIMIT_EXCEEDED',
      message: 'Too many submissions from this client. Please wait before submitting more reports.',
      details: { retry_after_seconds: Math.ceil((bucket.resetTime - now) / 1000) }
    });
    return;
  }

  next();
};
