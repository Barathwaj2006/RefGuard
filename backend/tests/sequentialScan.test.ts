import request from 'supertest';
import app from '../src/app';

describe('Sequential Scan Integrity (Memory/State Leak Test)', () => {
  it('should not leak state between sequential scans', async () => {
    // 1. Scan a HIGH risk message
    const highRiskPayload = {
      content_type: 'TEXT',
      content_value: 'Congratulations! You won the lottery. Click upi://pay?pa=scammer@ybl&am=5000 to collect your prize by entering PIN.',
      source_context: 'SMS',
      timestamp: new Date().toISOString()
    };

    const res1 = await request(app)
      .post('/api/v1/scan')
      .send(highRiskPayload)
      .expect(200);

    expect(res1.body.risk_assessment.risk_severity).toBe('CRITICAL');
    expect(res1.body.evidence_pack.items.length).toBeGreaterThan(2);

    // 2. Scan a benign message immediately after
    const benignPayload = {
      content_type: 'TEXT',
      content_value: 'Hey, are we still on for lunch?',
      source_context: 'WhatsApp',
      timestamp: new Date().toISOString()
    };

    const res2 = await request(app)
      .post('/api/v1/scan')
      .send(benignPayload)
      .expect(200);

    // If state leaks, risk_severity might be high, or evidence from scan 1 might be in scan 2
    expect(res2.body.risk_assessment.risk_severity).toBe('LOW');
    expect(res2.body.risk_assessment.risk_score).toBe(10);
    expect(res2.body.evidence_pack.items.length).toBeLessThan(res1.body.evidence_pack.items.length);
    
    // Check that evidence from scan 1 doesn't appear in scan 2
    const hasUpiEvidence = res2.body.evidence_pack.items.some((item: any) => item.evidence_type === 'UPI_IDENTIFIER');
    expect(hasUpiEvidence).toBe(false);

    // Check that scan_id is different
    expect(res1.body.scan_id).not.toBe(res2.body.scan_id);
    expect(res1.body.evidence_pack.incident_id).not.toBe(res2.body.evidence_pack.incident_id);
  });
});
