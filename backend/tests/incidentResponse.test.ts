import request from 'supertest';
import app from '../src/app';

describe('Incident Response Intelligence', () => {
  it('should generate BENIGN response for a safe transaction', async () => {
    // First, scan
    const scanRes = await request(app)
      .post('/api/v1/scan')
      .send({
        content_type: 'TEXT',
        content_value: 'Hey, sending 500 for lunch.',
        source_context: 'com.whatsapp',
        timestamp: new Date().toISOString()
      });

    // Then get recommendation
    const res = await request(app)
      .post('/api/v1/incident/recommendation')
      .send(scanRes.body)
      .expect(200);

    const rec = res.body.incident_recommendation;
    expect(rec.incident_category).toBe('BENIGN');
    expect(rec.urgency).toBe('LOW');
  });

  it('should generate TRADING_FRAUD response', async () => {
    const scanRes = await request(app)
      .post('/api/v1/scan')
      .send({
        content_type: 'TEXT',
        content_value: 'Join my Telegram VIP group for guaranteed crypto returns!',
        source_context: 'org.telegram.messenger',
        timestamp: new Date().toISOString()
      });

    const res = await request(app)
      .post('/api/v1/incident/recommendation')
      .send(scanRes.body)
      .expect(200);

    const rec = res.body.incident_recommendation;
    expect(rec.incident_category).toBe('TRADING_FRAUD');
    expect(rec.reporting_destination).toContain('SEBI');
    expect(rec.supporting_evidence_references.length).toBeGreaterThan(0);
  });

  it('should generate UPI_FRAUD response', async () => {
    const scanRes = await request(app)
      .post('/api/v1/scan')
      .send({
        content_type: 'TEXT',
        content_value: 'Urgent: electricity bill pending. Please pay immediately or power will be disconnected. upi://pay?pa=scammer@ybl',
        source_context: 'com.whatsapp',
        timestamp: new Date().toISOString()
      });

    const res = await request(app)
      .post('/api/v1/incident/recommendation')
      .send(scanRes.body)
      .expect(200);

    const rec = res.body.incident_recommendation;
    expect(rec.incident_category).toBe('UPI_FRAUD');
    expect(rec.immediate_action).toContain('do not enter your UPI PIN');
  });

  it('should generate AUTHORITY_IMPERSONATION response', async () => {
    const scanRes = await request(app)
      .post('/api/v1/scan')
      .send({
        content_type: 'TEXT',
        content_value: 'This is Police. Your parcel was seized by customs. Send money immediately to avoid digital arrest.',
        source_context: 'com.whatsapp',
        timestamp: new Date().toISOString()
      });

    const res = await request(app)
      .post('/api/v1/incident/recommendation')
      .send(scanRes.body)
      .expect(200);

    const rec = res.body.incident_recommendation;
    expect(rec.incident_category).toBe('AUTHORITY_IMPERSONATION');
    expect(rec.reporting_reason).toContain('impersonation of government officials');
  });

  it('should generate KYC_ACCOUNT_TAKEOVER response', async () => {
    const scanRes = await request(app)
      .post('/api/v1/scan')
      .send({
        content_type: 'TEXT',
        content_value: 'Your bank account will be blocked. Share OTP to update KYC immediately.',
        source_context: 'com.android.mms',
        timestamp: new Date().toISOString()
      });

    const res = await request(app)
      .post('/api/v1/incident/recommendation')
      .send(scanRes.body)
      .expect(200);

    const rec = res.body.incident_recommendation;
    expect(rec.incident_category).toBe('KYC_ACCOUNT_TAKEOVER');
    expect(rec.evidence_preservation_guidance).toContain('SMS requesting KYC update');
  });

  it('should handle incomplete scam chain and missing evidence gracefully', async () => {
    // Artificial ScanResponse with missing evidence references
    const dummyScanResponse = {
      scan_id: '123',
      timestamp: new Date().toISOString(),
      risk_assessment: {
        risk_score: 90,
        risk_severity: 'CRITICAL',
        confidence: 0.9,
        signals: ['suspicious_activity_unknown'],
        human_explanation: 'Suspicious',
        recommended_action: 'Stop'
      },
      protection_decision: {
        action: 'DISCOURAGE_PROCEED',
        detected_summary: 'Threat',
        why_it_matters: 'It matters',
        user_instruction: 'Stop'
      }
    };

    const res = await request(app)
      .post('/api/v1/incident/recommendation')
      .send(dummyScanResponse)
      .expect(200);

    const rec = res.body.incident_recommendation;
    expect(rec.incident_category).toBe('UNKNOWN');
    expect(rec.supporting_evidence_references.length).toBe(0);
  });

  it('should reject invalid payload', async () => {
    const res = await request(app)
      .post('/api/v1/incident/recommendation')
      .send({ some_garbage: 'data' })
      .expect(400);

    expect(res.body.error_code).toBe('INVALID_PAYLOAD');
  });
});
