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

const scanBuckets = new Map<string, RequestBucket>();
const SCAN_WINDOW_MS = 60 * 1000;
const MAX_SCANS_PER_WINDOW = 60; // Max 60 scans per IP/minute

// Periodic cleanup to prevent memory leaks from unused IPs
setInterval(() => {
  const now = Date.now();
  for (const [ip, bucket] of ipBuckets.entries()) {
    if (now > bucket.resetTime) {
      ipBuckets.delete(ip);
    }
  }
  for (const [ip, bucket] of scanBuckets.entries()) {
    if (now > bucket.resetTime) {
      scanBuckets.delete(ip);
    }
  }
}, 5 * 60 * 1000).unref(); // Run every 5 mins, don't block event loop

export const scanRateLimiter = (req: Request, res: Response, next: NextFunction): void => {
  const ip = req.ip || req.socket.remoteAddress || 'unknown_ip';
  const now = Date.now();

  let bucket = scanBuckets.get(ip);
  if (!bucket || now > bucket.resetTime) {
    bucket = { count: 1, resetTime: now + SCAN_WINDOW_MS };
    scanBuckets.set(ip, bucket);
    return next();
  }

  bucket.count++;
  if (bucket.count > MAX_SCANS_PER_WINDOW) {
    res.status(429).json({
      error_code: 'RATE_LIMIT_EXCEEDED',
      message: 'Too many scan requests from this client. Please wait before submitting more.',
      details: { retry_after_seconds: Math.ceil((bucket.resetTime - now) / 1000) }
    });
    return;
  }

  next();
};
