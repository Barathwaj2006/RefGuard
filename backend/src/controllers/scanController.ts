import { Request, Response, NextFunction } from 'express';
import { AnalyzerService } from '../services/analyzer';
import { ScanRequest } from '../models/types';

const analyzer = new AnalyzerService();

// Match explicit credential leaks (e.g. My UPI PIN is 1234, password: xyz, cvv: 123)
const RAW_CREDENTIAL_PATTERN = /\b(?:my\s+)?(?:upi\s+|atm\s+)?pin\s*(?:is|:|=)\s*\d{4,8}\b|\b(?:my\s+)?(?:password|passwd|pwd)\s*(?:is|:|=)\s*\S+|\bcvv\s*(?:is|:|=)?\s*\d{3,4}\b/i;

export const scanContent = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const scanReq: ScanRequest = req.body;

    // Reject raw credentials (but allow PII through sanitizer)
    if (RAW_CREDENTIAL_PATTERN.test(scanReq.content_value)) {
      return res.status(400).json({
        error_code: 'SENSITIVE_DATA_REJECTED',
        error_message: 'Payload contains potentially sensitive credentials',
      });
    }

    // timeout handling check with proper cleanup to prevent timer leaks
    const timeoutMs = 15000;
    let timer: NodeJS.Timeout;
    const timeoutPromise = new Promise((_, reject) => {
      timer = setTimeout(() => reject(new Error('ANALYSIS_TIMEOUT')), timeoutMs);
    });

    let response: any;
    try {
      response = await Promise.race([
        analyzer.analyze(scanReq),
        timeoutPromise
      ]);
    } finally {
      clearTimeout(timer!);
    }

    // Add Gemini header if present in evidence pack or somewhere
    const isGeminiUsed = response.evidence_pack && response.evidence_pack.items 
      ? response.evidence_pack.items.some(
          (item: any) => item.evidence_type === 'RISK_SIGNAL' && item.data.includes('gemini_reasoning_applied')
        )
      : false;
    
    res.setHeader('X-Gemini-Used', isGeminiUsed ? 'true' : 'false');
    res.status(200).json(response);
  } catch (error) {
    next(error);
  }
};

