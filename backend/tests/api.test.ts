import request from 'supertest';
import app from '../src/app';

// We need to inject a test route to test the error handler and validation edges
import { Router } from 'express';
import { validateRequest, validateResponse } from '../src/middleware/validator';
import { errorHandler } from '../src/middleware/errorHandler';
import * as scanController from '../src/controllers/scanController';
import * as reportController from '../src/controllers/reportController';

const testRouter = Router();

// Test route to trigger missing schema
testRouter.post('/missing-schema-req', validateRequest('non-existent-schema.json'), (req, res) => {
  res.status(200).send();
});
testRouter.post('/missing-schema-res', validateResponse('non-existent-schema.json'), (req, res) => {
  res.status(200).send();
});

// Test route to trigger invalid response validation
testRouter.post('/invalid-response-schema', validateResponse('scan-response.json'), (req, res) => {
  // send an invalid response that doesn't match scan-response.json
  res.status(200).json({ invalid_field: "This is wrong" });
});

// Create a route that throws an error explicitly triggering global error handler
testRouter.post('/trigger-error', (req, res, next) => {
  next(new Error('Test Error'));
});

// Create routes to trigger catch blocks in controllers directly
testRouter.post('/trigger-scan-error', (req, res, next) => {
  // Mock req.body.content_value.match to throw an error
  const badReq = { ...req, body: { content_value: { match: () => { throw new Error('Mocked error'); } } } } as any;
  scanController.scanContent(badReq, res, next);
});

testRouter.post('/trigger-report-error', (req, res, next) => {
  // Mock req.body so that report.report_id throws an error or just pass a bad res object
  const badRes = { status: () => { throw new Error('Mocked error'); } } as any;
  reportController.submitReport(req, badRes, next);
});


// Add the test router to app
app.use('/test', testRouter);

// Need to ensure the error handler is attached AFTER the test routes so it handles errors from them
app.use(errorHandler);

describe('RefGuard API v1 Foundation', () => {
  describe('POST /api/v1/scan', () => {
    it('should successfully process a valid scan request with HIGH risk', async () => {
      const payload = {
        content_type: 'TEXT',
        content_value: 'Hello, is this a scam?',
        source_context: 'com.whatsapp',
        timestamp: new Date().toISOString()
      };

      const res = await request(app)
        .post('/api/v1/scan')
        .send(payload)
        .expect(200);

      expect(res.body).toHaveProperty('scan_id');
      expect(res.body).toHaveProperty('timestamp');
      expect(res.body).toHaveProperty('risk_assessment');
      expect(res.body).toHaveProperty('protection_decision');

      expect(res.body.risk_assessment.risk_severity).toBe('HIGH');
      expect(res.body.protection_decision.action).toBe('DISCOURAGE_PROCEED');
    });

    it('should successfully process a valid scan request with LOW risk', async () => {
      const payload = {
        content_type: 'TEXT',
        content_value: 'Hello, how are you?',
        source_context: 'com.whatsapp',
        timestamp: new Date().toISOString()
      };

      const res = await request(app)
        .post('/api/v1/scan')
        .send(payload)
        .expect(200);

      expect(res.body.risk_assessment.risk_severity).toBe('LOW');
      expect(res.body.protection_decision.action).toBe('ALLOW');
    });

    it('should return a structured 400 error for an invalid payload (missing timestamp)', async () => {
      const invalidPayload = {
        content_type: 'URL',
        content_value: 'http://example.com'
      };

      const res = await request(app)
        .post('/api/v1/scan')
        .send(invalidPayload)
        .expect(400);

      expect(res.body).toHaveProperty('error_code', 'INVALID_REQUEST');
      expect(res.body).toHaveProperty('error_message');
      expect(res.body).toHaveProperty('details');
      expect(res.body.details).toContain('must have required property \'timestamp\'');
    });

    it('should return a structured 400 error for invalid content_type enum', async () => {
       const invalidEnumPayload = {
        content_type: 'INVALID_TYPE',
        content_value: 'test',
        timestamp: new Date().toISOString()
      };

      const res = await request(app)
        .post('/api/v1/scan')
        .send(invalidEnumPayload)
        .expect(400);

      expect(res.body).toHaveProperty('error_code', 'INVALID_REQUEST');
      expect(res.body.details).toContain('must be equal to one of the allowed values');
    });

    it('should block explicit credentials/PINs immediately', async () => {
      const sensitivePayload = {
        content_type: 'TEXT',
        content_value: 'My UPI PIN is 1234',
        timestamp: new Date().toISOString()
      };

      const res = await request(app)
        .post('/api/v1/scan')
        .send(sensitivePayload)
        .expect(400);

      expect(res.body).toHaveProperty('error_code', 'SENSITIVE_DATA_REJECTED');
    });

    it('should return 400 for malformed JSON request', async () => {
      const res = await request(app)
        .post('/api/v1/scan')
        .set('Content-Type', 'application/json')
        .send('{"content_type": "TEXT", "content_value": "missing brace"')
        .expect(400);

      expect(res.body).toHaveProperty('error_code', 'MALFORMED_REQUEST');
    });

    it('should handle internal errors gracefully in scanController', async () => {
      const res = await request(app)
        .post('/test/trigger-scan-error')
        .send({})
        .expect(500);

      expect(res.body).toHaveProperty('error_code', 'INTERNAL_SERVER_ERROR');
    });
  });

  describe('POST /api/v1/report', () => {
    it('should successfully accept a valid community report', async () => {
      const reportPayload = {
        report_id: 'rep-001',
        reported_indicator: 'bad-upi@upi',
        report_category: 'UPI_FRAUD',
        submission_timestamp: new Date().toISOString(),
        moderation_status: 'PENDING',
        confidence: 0.8,
        provenance: 'USER_SUBMISSION'
      };

      const res = await request(app)
        .post('/api/v1/report')
        .send(reportPayload)
        .expect(200);

      expect(res.body).toHaveProperty('report_id', 'rep-001');
      expect(res.body).toHaveProperty('status', 'RECEIVED');
    });

    it('should reject an invalid report missing required fields', async () => {
      const invalidReport = {
        report_id: 'rep-002',
        // missing reported_indicator
      };

      const res = await request(app)
        .post('/api/v1/report')
        .send(invalidReport)
        .expect(400);

      expect(res.body).toHaveProperty('error_code', 'INVALID_REQUEST');
    });

    it('should handle internal errors gracefully in reportController', async () => {
      const res = await request(app)
        .post('/test/trigger-report-error')
        .send({})
        .expect(500);

      expect(res.body).toHaveProperty('error_code', 'INTERNAL_SERVER_ERROR');
    });
  });

  describe('Global Error Handling and Validation Edge Cases', () => {
    it('should return 500 INTERNAL_SERVER_ERROR for unhandled exceptions', async () => {
      const res = await request(app)
        .post('/test/trigger-error')
        .send({})
        .expect(500);
      expect(res.body).toHaveProperty('error_code', 'INTERNAL_SERVER_ERROR');
    });

    it('should return 500 when request schema is not found', async () => {
      const res = await request(app)
        .post('/test/missing-schema-req')
        .send({})
        .expect(500);
      expect(res.body).toHaveProperty('error_code', 'INTERNAL_ERROR');
      expect(res.body.error_message).toContain('Schema not found');
    });

    it('should return 500 when response schema is not found', async () => {
      const res = await request(app)
        .post('/test/missing-schema-res')
        .send({})
        .expect(500);
      expect(res.body).toHaveProperty('error_code', 'INTERNAL_ERROR');
      expect(res.body.error_message).toContain('Schema not found');
    });

    it('should return 500 when response violates schema contract', async () => {
      // Suppress console.error for this expected error test
      const originalConsoleError = console.error;
      console.error = jest.fn();

      const res = await request(app)
        .post('/test/invalid-response-schema')
        .send({})
        .expect(500);
      expect(res.body).toHaveProperty('error_code', 'INTERNAL_SERVER_ERROR');
      expect(res.body.error_message).toContain('Response validation failed against schema contract');

      console.error = originalConsoleError;
    });
  });
});
