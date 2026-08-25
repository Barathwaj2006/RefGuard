import request from 'supertest';
import app from '../src/app';

describe('Source-Aware Fraud Intelligence', () => {
  it('should elevate risk for Telegram trading scams', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Join our premium VIP group for 200% guaranteed returns. Deposit 5000 rs now.',
      source_context: 'org.telegram.messenger',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    expect(res.body.risk_assessment.risk_severity).toBe('CRITICAL');
    expect(res.body.risk_assessment.signals).toContain('telegram_trading_scam');
    expect(res.body.scam_chain.nodes[0].entity_reference).toBe('Telegram Message');
  });

  it('should elevate risk for WhatsApp emergency imposter scams', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Bhai accident ho gaya hai hospital me hu. Urgent 10000 rs gpay kar de please.',
      source_context: 'com.whatsapp',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    expect(res.body.risk_assessment.risk_severity).toBe('CRITICAL');
    expect(res.body.risk_assessment.signals).toContain('whatsapp_imposter_emergency');
    expect(res.body.scam_chain.nodes[0].entity_reference).toBe('WhatsApp Message');
  });

  it('should elevate risk for SMS authority impersonation (Electricity)', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Dear customer your electricity power will be disconnected tonight. Update bill immediately.',
      source_context: 'com.android.mms',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    expect(res.body.risk_assessment.risk_severity).toBe('CRITICAL');
    expect(res.body.risk_assessment.signals).toContain('sms_authority_impersonation');
    expect(res.body.scam_chain.nodes[0].entity_reference).toBe('SMS Message');
  });

  it('should not elevate risk for benign financial SMS', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Your a/c XX1234 is credited with Rs 5,000 on 20-Aug. Clear balance is Rs 10,000.',
      source_context: 'com.android.mms',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    // Should remain low risk despite being SMS and having money
    expect(res.body.risk_assessment.risk_severity).toBe('LOW');
    expect(res.body.risk_assessment.signals).not.toContain('sms_authority_impersonation');
    expect(res.body.scam_chain.nodes[0].entity_reference).toBe('SMS Message');
  });

  it('should correctly identify Web Browser source context', async () => {
    const payload = {
      content_type: 'URL',
      content_value: 'https://fake-lottery-winner.xyz/claim',
      source_context: 'com.android.chrome',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    // It correctly hits CRITICAL now because both suspicious TLD and urgency words ("lottery", "winner") trigger
    expect(res.body.risk_assessment.risk_severity).toBe('CRITICAL');
    expect(res.body.scam_chain.nodes[0].entity_reference).toBe('Web Browser Message');
  });

  it('should correctly handle Unknown source context', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Hello there, how are you?',
      source_context: 'some.unknown.app',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    expect(res.body.risk_assessment.risk_severity).toBe('LOW');
    expect(res.body.scam_chain.nodes[0].entity_reference).toBe('Unknown Message');
  });
});
