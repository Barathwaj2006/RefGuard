import { Request, Response, NextFunction } from 'express';
import { AnalyzerService } from '../services/analyzer';
import { ScanRequest } from '../models/types';

const analyzer = new AnalyzerService();

export const scanContent = async (req: Request, res: Response, next: NextFunction) => {
  try {
    const scanReq: ScanRequest = req.body;

    // Explicitly reject credentials in normal scan flows
    if (scanReq.content_value.match(/password|pin|cvv/i)) {
      return res.status(400).json({
        error_code: 'SENSITIVE_DATA_REJECTED',
        error_message: 'Payload contains potentially sensitive credentials',
      });
    }

    const response = await analyzer.analyze(scanReq);
    res.status(200).json(response);
  } catch (error) {
    next(error);
  }
};
