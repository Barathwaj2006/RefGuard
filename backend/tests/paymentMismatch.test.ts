import request from 'supertest';
import app from '../src/app';

describe('Payment Intent Mismatch', () => {
  it('should detect a mismatch when claiming a refund requires UPI pin entry', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Your refund of Rs 500 is ready. Enter your UPI pin to collect money upi://pay?pa=scammer@sbi&am=500',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    expect(res.body.payment_intent_mismatch).toBeDefined();
    expect(res.body.payment_intent_mismatch.status).toBe('DETECTED');
    expect(res.body.payment_intent_mismatch.stated_intent).toBe('RECEIVE_FUNDS_OR_PRIZE');
    expect(res.body.payment_intent_mismatch.actual_payment_action).toBe('OUTBOUND_DEBIT_COLLECT');
    expect(res.body.protection_decision.action).toBe('DISCOURAGE_PROCEED');
  });

  it('should detect a mismatch for account verification asking for Rs 1 debit', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Please verify your account by sending Rs 1 to upi://pay?pa=verify-desk@icici&am=1',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    expect(res.body.payment_intent_mismatch).toBeDefined();
    expect(res.body.payment_intent_mismatch.status).toBe('DETECTED');
    expect(res.body.payment_intent_mismatch.stated_intent).toBe('ACCOUNT_VERIFICATION');
    expect(res.body.payment_intent_mismatch.actual_payment_action).toBe('OUTBOUND_DEBIT_COLLECT');
    expect(res.body.protection_decision.action).toBe('DISCOURAGE_PROCEED');
    expect(res.body.protection_decision.why_it_matters).toContain('You were told this is to verify your account, but this action will actually DEBIT');
  });

  it('should NOT flag a legitimate electricity bill payment', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Your electricity bill of Rs 1500 is due. Please pay now upi://pay?pa=bescom@sbi&am=1500',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    expect(res.body.payment_intent_mismatch).toBeDefined();
    expect(res.body.payment_intent_mismatch.status).toBe('NOT_DETECTED');
    expect(res.body.payment_intent_mismatch.stated_intent).toBe('STANDARD_PAYMENT');
    expect(res.body.payment_intent_mismatch.actual_payment_action).toBe('OUTBOUND_DEBIT_COLLECT');
  });

  it('should NOT flag a legitimate receiving money notification without an action', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'You have received a refund of Rs 500 to your account.',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    expect(res.body.payment_intent_mismatch).toBeDefined();
    expect(res.body.payment_intent_mismatch.status).toBe('NOT_DETECTED');
    expect(res.body.payment_intent_mismatch.stated_intent).toBe('RECEIVE_FUNDS_OR_PRIZE');
    expect(res.body.payment_intent_mismatch.actual_payment_action).toBe('NONE');
  });
});
