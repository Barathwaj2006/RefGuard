import { Request, Response, NextFunction } from 'express';
import { ErrorResponse } from '../models/types';

export const errorHandler = (
  err: unknown,
  _req: Request,
  res: Response,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  next: NextFunction
) => {
  if (err instanceof SyntaxError && typeof err === 'object' && err !== null && 'body' in err) {
    const errorResponse: ErrorResponse = {
      error_code: 'MALFORMED_REQUEST',
      error_message: 'Invalid JSON payload',
    };
    return res.status(400).json(errorResponse);
  }

  if (err instanceof Error) {
    if (err.message === 'ANALYSIS_TIMEOUT') {
      return res.status(408).json({
        error_code: 'ANALYSIS_TIMEOUT',
        error_message: 'The analysis took too long to complete',
      });
    }

    if (err.message.includes('GoogleGenAI') || err.message.includes('Gemini')) {
      // Gemini specific errors should not crash the app, but here it's caught at the controller level
      // if it leaked out for some reason.
      return res.status(503).json({
        error_code: 'AI_SERVICE_UNAVAILABLE',
        error_message: 'AI reasoning service is temporarily unavailable',
      });
    }
  }

  console.error('Unhandled Error:', err);

  const errorResponse: ErrorResponse = {
    error_code: 'INTERNAL_SERVER_ERROR',
    error_message: 'An unexpected error occurred',
  };

  res.status(500).json(errorResponse);
};
