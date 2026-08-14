import { Router } from 'express';
import { scanContent } from '../controllers/scanController';
import { submitReport } from '../controllers/reportController';
import { validateRequest, validateResponse } from '../middleware/validator';

const router = Router();

router.post('/scan', validateRequest('scan-request.json'), validateResponse('scan-response.json'), scanContent);
// Report endpoint response is minimal and inline in the contract API, so we skip response validation here.
router.post('/report', validateRequest('scam-report.json'), submitReport);

export default router;
