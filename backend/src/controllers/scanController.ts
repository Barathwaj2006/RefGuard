import { Request, Response, NextFunction } from 'express';
import { AnalyzerService } from '../services/analyzer';
import { ScanRequest } from '../models/types';
import { sanitizeText } from '../services/extractors/piiSanitizer';

const analyzer = new AnalyzerService();

export const scanContent = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const scanReq: ScanRequest = req.body;

    // Reject raw credentials (but allow PII through sanitizer)
    if (scanReq.content_value.match(/password|pin|cvv/i)) {
      return res.status(400).json({
        error_code: 'SENSITIVE_DATA_REJECTED',
        error_message: 'Payload contains potentially sensitive credentials',
      });
    }

    // timeout handling check
    const timeoutMs = 15000;
    const timeoutPromise = new Promise((_, reject) => 
      setTimeout(() => reject(new Error('ANALYSIS_TIMEOUT')), timeoutMs)
    );

    const response = await Promise.race([
      analyzer.analyze(scanReq),
      timeoutPromise
    ]) as any;

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
