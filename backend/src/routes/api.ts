import { Router, Request, Response } from 'express';
import { scanContent } from '../controllers/scanController';
import { submitReport } from '../controllers/reportController';
import { validateRequest, validateResponse } from '../middleware/validator';
import { communityStore } from '../services/communityStore';

const router = Router();

// Health Check
router.get('/health', (_req: Request, res: Response) => {
  res.status(200).json({
    status: 'HEALTHY',
    version: '1.0.0',
    service: 'RefGuard Backend API',
    uptime_seconds: process.uptime(),
    community_indicators_loaded: communityStore.getCount() + 5
  });
});

router.post('/scan', validateRequest('scan-request.json'), validateResponse('scan-response.json'), scanContent);
router.post('/report', validateRequest('scam-report.json'), submitReport);

export default router;
