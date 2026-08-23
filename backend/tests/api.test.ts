import request from 'supertest';
import app from '../src/app';

describe('RefGuard API v1 Foundation', () => {
  describe('Health Endpoints', () => {
    it('should return 200 for root GET /health', async () => {
      const res = await request(app).get('/health').expect(200);
      expect(res.body).toEqual({ status: 'UP', service: 'RefGuard' });
    });

    it('should return 200 and diagnostics for GET /api/v1/health', async () => {
      const res = await request(app).get('/api/v1/health').expect(200);
      expect(res.body).toHaveProperty('status', 'HEALTHY');
      expect(res.body).toHaveProperty('version', '1.0.0');
      expect(res.body).toHaveProperty('community_indicators_loaded');
    });
  });

  describe('POST /api/v1/scan', () => {
    it('should successfully process a valid scan request and return complete contract models', async () => {
      const payload = {
        content_type: 'TEXT',
        content_value: 'Congratulations! You won lottery reward. Pay collect request now upi://pay?pa=fake-cashback-reward@paytm&am=500',
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
      expect(res.body).toHaveProperty('payment_intent_mismatch');
      expect(res.body).toHaveProperty('scam_chain');
      expect(res.body).toHaveProperty('evidence_pack');

      expect(res.body.risk_assessment.risk_severity).toBe('CRITICAL');
      expect(res.body.protection_decision.action).toBe('DISCOURAGE_PROCEED');
      expect(res.body.payment_intent_mismatch.status).toBe('DETECTED');
      expect(res.body.evidence_pack.items.length).toBeGreaterThan(0);
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

    it('should flag trading scam messages appropriately', async () => {
      const payload = {
        content_type: 'TEXT',
        content_value: 'Open an account with Angel Broking and get 50% guaranteed returns weekly. Deposit ₹10,000 to start trading with us.',
        timestamp: new Date().toISOString()
      };

      const res = await request(app)
        .post('/api/v1/scan')
        .send(payload)
        .expect(200);

      expect(res.body.risk_assessment.risk_severity).toBe('CRITICAL');
      expect(res.body.risk_assessment.signals).toContain('fake_broker_reference');
      expect(res.body.risk_assessment.signals).toContain('guaranteed_return_claim');
      expect(res.body.risk_assessment.signals).toContain('deposit_payment_request');
    });

    it('should ignore benign trading discussion', async () => {
      const payload = {
        content_type: 'TEXT',
        content_value: 'Do you think it is a good time to buy more shares in that tech company? The market is down.',
        timestamp: new Date().toISOString()
      };

      const res = await request(app)
        .post('/api/v1/scan')
        .send(payload)
        .expect(200);

      expect(res.body.risk_assessment.risk_severity).toBe('LOW');
      expect(res.body.protection_decision.action).toBe('ALLOW');
    });

    it('should detect crypto wallet scams', async () => {
      const payload = {
        content_type: 'TEXT',
        content_value: 'Deposit ₹100 to activate your account. Send to 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
        timestamp: new Date().toISOString()
      };

      const res = await request(app)
        .post('/api/v1/scan')
        .send(payload)
        .expect(200);

      expect(res.body.risk_assessment.risk_severity).toBe('CRITICAL');
      expect(res.body.risk_assessment.signals).toContain('crypto_wallet_address');
    });

    it('should detect KYC phishing in trading context', async () => {
      const payload = {
        content_type: 'TEXT',
        content_value: 'I am a SEBI registered broker. Share your PAN and Aadhaar for KYC immediately.',
        timestamp: new Date().toISOString()
      };

      const res = await request(app)
        .post('/api/v1/scan')
        .send(payload)
        .expect(200);

      expect(res.body.risk_assessment.risk_severity).toBe('CRITICAL');
      expect(res.body.risk_assessment.signals).toContain('sebi_reference');
      expect(res.body.risk_assessment.signals).toContain('kyc_phishing');
    });

    it('should successfully fallback when Gemini is unavailable on ambiguous messages', async () => {
      const originalApiKey = process.env.GEMINI_API_KEY;
      delete process.env.GEMINI_API_KEY; // Force fallback

      const payload = {
        content_type: 'TEXT',
        content_value: 'Urgent action required! Claim your cashback now.', // 55 score (Medium) - triggers escalation
        timestamp: new Date().toISOString()
      };

      const res = await request(app)
        .post('/api/v1/scan')
        .send(payload)
        .expect(200);

      expect(res.body.risk_assessment.risk_severity).toBe('MEDIUM');
      expect(res.header['x-gemini-used']).toBe('false');

      if (originalApiKey) process.env.GEMINI_API_KEY = originalApiKey;
    });
  });

  describe('POST /api/v1/report & Dynamic Threat Memory', () => {
    it('should successfully accept a valid community report', async () => {
      const reportPayload = {
        report_id: 'rep-001',
        reported_indicator: 'new-fraud-seller@okaxis',
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

    it('should detect previously reported indicator in subsequent scan', async () => {
      const scanReq = {
        content_type: 'UPI_VPA',
        content_value: 'new-fraud-seller@okaxis',
        timestamp: new Date().toISOString()
      };

      const scanRes = await request(app)
        .post('/api/v1/scan')
        .send(scanReq)
        .expect(200);

      expect(scanRes.body.risk_assessment.risk_severity).toBe('CRITICAL');
      expect(scanRes.body.risk_assessment.signals).toContain('community_blacklist_match');
    });

    it('should reject an invalid report missing required fields', async () => {
      const invalidReport = {
        report_id: 'rep-002',
      };

      const res = await request(app)
        .post('/api/v1/report')
        .send(invalidReport)
        .expect(400);

      expect(res.body).toHaveProperty('error_code', 'INVALID_REQUEST');
    });

    it('should sanitize PII from report descriptions and evidence references', async () => {
      const reportPayload = {
        report_id: 'rep-pii-001',
        reported_indicator: 'scammer@upi',
        report_category: 'UPI_FRAUD',
        description: 'The scammer called me from 9876543210 and asked for my Aadhaar 1234-5678-9012.',
        evidence_references: ['Here is the email: victim@gmail.com'],
        submission_timestamp: new Date().toISOString(),
        moderation_status: 'PENDING',
        confidence: 0.9,
        provenance: 'USER_SUBMISSION'
      };

      const res = await request(app)
        .post('/api/v1/report')
        .send(reportPayload)
        .expect(200);

      expect(res.body.status).toBe('RECEIVED');

      // Verify via intel/reports endpoint
      const intelRes = await request(app).get('/api/v1/intel/reports?limit=5');
      const sanitizedReport = intelRes.body.recent_reports.find((r: any) => r.report_id === 'rep-pii-001');
      
      expect(sanitizedReport).toBeDefined();
      expect(sanitizedReport.description).toContain('XXXXXX3210');
      expect(sanitizedReport.description).toContain('[AADHAAR_REDACTED]');
      expect(sanitizedReport.description).not.toContain('9876543210');
      expect(sanitizedReport.description).not.toContain('1234-5678-9012');
    });
  });
});
