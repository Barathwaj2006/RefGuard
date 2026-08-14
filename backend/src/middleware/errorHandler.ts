import { Request, Response, NextFunction } from 'express';
import { ErrorResponse } from '../models/types';

export const errorHandler = (
  err: any,
  req: Request,
  res: Response,
  next: NextFunction
) => {
  if (err instanceof SyntaxError && 'body' in err) {
    const errorResponse: ErrorResponse = {
      error_code: 'MALFORMED_REQUEST',
      error_message: 'Invalid JSON payload',
    };
    return res.status(400).json(errorResponse);
  }

  const errorResponse: ErrorResponse = {
    error_code: 'INTERNAL_SERVER_ERROR',
    error_message: 'An unexpected error occurred',
  };

  res.status(500).json(errorResponse);
};
