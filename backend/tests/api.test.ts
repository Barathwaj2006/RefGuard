import request from 'supertest';
import app from '../src/app';

describe('RefGuard API v1 Foundation', () => {
  describe('POST /api/v1/scan', () => {
    it('should successfully process a valid scan request', async () => {
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
  });
});
