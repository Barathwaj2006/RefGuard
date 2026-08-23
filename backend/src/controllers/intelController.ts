import { Request, Response, NextFunction } from 'express';
import { communityStore } from '../services/communityStore';

export const getTrendingIntel = (req: Request, res: Response, next: NextFunction) => {
  try {
    const limit = parseInt(req.query.limit as string) || 10;
    const trending = communityStore.getTrendingIndicators(limit);
    
    res.status(200).json({
      status: 'SUCCESS',
      count: trending.length,
      trending_indicators: trending.map(record => ({
        indicator: record.indicator,
        report_count: record.reportCount,
        first_reported_at: record.firstReportedAt,
        last_reported_at: record.lastReportedAt,
        source: record.source
      }))
    });
  } catch (error) {
    next(error);
  }
};

export const getRecentReports = (req: Request, res: Response, next: NextFunction) => {
  try {
    const limit = parseInt(req.query.limit as string) || 20;
    const reports = communityStore.getRecentReports(limit);

    res.status(200).json({
      status: 'SUCCESS',
      count: reports.length,
      recent_reports: reports.map(report => ({
        report_id: report.report_id,
        indicator: report.reported_indicator,
        category: report.report_category,
        description: report.description,
        timestamp: report.submission_timestamp,
        provenance: report.provenance
      }))
    });
  } catch (error) {
    next(error);
  }
};
