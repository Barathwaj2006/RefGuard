import { Request, Response } from 'express';
import { incidentResponseService } from '../services/incidentResponseService';
import { ScanResponse } from '../models/types';

export const getIncidentRecommendation = (req: Request, res: Response): void => {
  try {
    const scanResponse: ScanResponse = req.body;
    
    if (!scanResponse || !scanResponse.risk_assessment) {
      res.status(400).json({
        error_code: 'INVALID_PAYLOAD',
        message: 'A valid ScanResponse with a risk_assessment is required.'
      });
      return;
    }

    const recommendation = incidentResponseService.generateRecommendation(scanResponse);
    
    res.status(200).json({
      status: 'SUCCESS',
      incident_recommendation: recommendation
    });
  } catch (error) {
    console.error('Error generating incident recommendation:', error);
    res.status(500).json({
      error_code: 'INTERNAL_ERROR',
      message: 'Failed to generate incident recommendation.'
    });
  }
};
