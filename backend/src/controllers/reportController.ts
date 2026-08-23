import { Request, Response, NextFunction } from 'express';
import { ScamReport } from '../models/types';
import { communityStore } from '../services/communityStore';
import { sanitizeText } from '../services/extractors/piiSanitizer';

export const submitReport = (req: Request, res: Response, next: NextFunction) => {
  try {
    const report: ScamReport = req.body;

    // Sanitize PII from free-text fields to prevent accidental exposure of victim data
    if (report.description) {
      report.description = sanitizeText(report.description).sanitizedText;
    }
    
    // Sanitize any free-text evidence references that might contain PII
    if (report.evidence_references && Array.isArray(report.evidence_references)) {
      report.evidence_references = report.evidence_references.map(
        ref => sanitizeText(ref).sanitizedText
      );
    }

    communityStore.addReport(report);

    res.status(200).json({
      report_id: report.report_id,
      status: 'RECEIVED'
    });
  } catch (error) {
    next(error);
  }
};
