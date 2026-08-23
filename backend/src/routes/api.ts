import { Router, Request, Response } from 'express';
import { scanContent } from '../controllers/scanController';
import { submitReport } from '../controllers/reportController';
import { validateRequest, validateResponse } from '../middleware/validator';
import { reportRateLimiter } from '../middleware/rateLimiter';
import { communityStore } from '../services/communityStore';

const router = Router();

// Health Check
router.get('/health', (_req: Request, res: Response) => {
  res.status(200).json({
    status: 'HEALTHY',
    version: '1.0.0',
    service: 'RefGuard Backend API',
    uptime_seconds: process.uptime(),
    community_indicators_loaded: communityStore.getCount() + 7,
    feedback_entries_logged: communityStore.getFeedbacksCount()
  });
});

// Scan Ingress
router.post('/scan', validateRequest('scan-request.json'), validateResponse('scan-response.json'), scanContent);

// Community Report Ingestion with Abuse Prevention & Rate Limiting
router.post('/report', reportRateLimiter, validateRequest('scam-report.json'), submitReport);

// Verdict Feedback Loop (False Alarm / Confirmed Scam)
router.post('/feedback', (req: Request, res: Response) => {
  const { scan_id, indicator, verdict, user_notes } = req.body;
  if (!scan_id || !verdict || !['CONFIRMED_FRAUD', 'FALSE_ALARM'].includes(verdict)) {
    res.status(400).json({
      error_code: 'INVALID_FEEDBACK_PAYLOAD',
      message: 'scan_id and valid verdict (CONFIRMED_FRAUD or FALSE_ALARM) are required.'
    });
    return;
  }

  communityStore.recordFeedback({
    scanId: scan_id,
    indicator,
    verdict,
    userNotes: user_notes,
    timestamp: new Date().toISOString()
  });

  res.status(200).json({
    status: 'RECORDED',
    scan_id,
    verdict,
    message: 'Feedback received and recorded for model calibration.'
  });
});

export default router;
