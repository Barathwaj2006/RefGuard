import { Request, Response, NextFunction } from 'express';
import { AnalyzerService } from '../services/analyzer';
import { ScanRequest, ScanResponse } from '../models/types';

// Lazy initialization to prevent timer leak if analyzer constructor throws
let _analyzerInstance: AnalyzerService | null = null;
const getAnalyzer = () => {
  if (!_analyzerInstance) {
    _analyzerInstance = new AnalyzerService();
  }
  return _analyzerInstance;
};

// Match explicit credential leaks (e.g. My UPI PIN is 1234, password: xyz, cvv: 123)
const RAW_CREDENTIAL_PATTERN = /\b(?:my\s+)?(?:upi\s+|atm\s+)?pin\s*(?:is|:|=)\s*\d{4,8}\b|\b(?:my\s+)?(?:password|passwd|pwd)\s*(?:is|:|=)\s*\S+|\bcvv\s*(?:is|:|=)?\s*\d{3,4}\b/i;

// Configurable timeout from environment variable with sensible default
const ANALYSIS_TIMEOUT_MS = parseInt(process.env.ANALYSIS_TIMEOUT_MS || '15000', 10);

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

    // Timeout handling with proper cleanup to prevent timer leaks
    let timer: NodeJS.Timeout | undefined;
    const timeoutPromise = new Promise<never>((_, reject) => {
      timer = setTimeout(() => reject(new Error('ANALYSIS_TIMEOUT')), ANALYSIS_TIMEOUT_MS);
    });

    let response: ScanResponse;
    try {
      response = (await Promise.race([
        getAnalyzer().analyze(scanReq),
        timeoutPromise
      ])) as ScanResponse;
    } finally {
      if (timer) {
        clearTimeout(timer);
      }
    }

    // Add Gemini header if present in evidence pack or somewhere
    const isGeminiUsed = response.evidence_pack && response.evidence_pack.items 
      ? response.evidence_pack.items.some(
          (item) => item.evidence_type === 'RISK_SIGNAL' && item.data.includes('gemini_reasoning_applied')
        )
      : false;
    
    res.setHeader('X-Gemini-Used', isGeminiUsed ? 'true' : 'false');
    res.status(200).json(response);
  } catch (error) {
    next(error);
  }
};

