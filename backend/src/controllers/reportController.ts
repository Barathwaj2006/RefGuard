import { Request, Response, NextFunction } from 'express';
import { ScamReport } from '../models/types';
import { communityStore } from '../services/communityStore';

export const submitReport = (req: Request, res: Response, next: NextFunction) => {
  try {
    const report: ScamReport = req.body;
    communityStore.addReport(report);

    res.status(200).json({
      report_id: report.report_id,
      status: 'RECEIVED'
    });
  } catch (error) {
    next(error);
  }
};
