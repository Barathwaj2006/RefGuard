import { Request, Response, NextFunction } from 'express';
import { ScamReport } from '../models/types';

export const submitReport = (req: Request, res: Response, next: NextFunction) => {
  try {
    const report: ScamReport = req.body;

    // Placeholder implementation for saving the report
    // Returns the success response as defined by the API contract
    res.status(200).json({
      report_id: report.report_id,
      status: 'RECEIVED'
    });
  } catch (error) {
    next(error);
  }
};
