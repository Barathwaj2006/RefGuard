import request from 'supertest';
import app from '../src/app';

describe('False Positive Regression Suite', () => {
  const evaluate = async (text: string, source: string, expectedSeverity: string[]) => {
    const payload = {
      content_type: 'TEXT',
      content_value: text,
      source_context: source,
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    expect(expectedSeverity).toContain(res.body.risk_assessment.risk_severity);
    return res;
  };

  describe('Legitimate (BENIGN) Scenarios', () => {
    it('CDSL Depository Alert', async () => {
      await evaluate(
        'CDSL: Debit of 100 shares of RELIANCE from demat a/c ***123 on 10-Aug. If not done by you, contact DP.',
        'com.android.mms',
        ['LOW']
      );
    });

    it('Legitimate Bank Payment Alert', async () => {
      await evaluate(
        'Dear Customer, Rs.500.00 has been debited from your A/c XX123 on 01-Sep. Info: UPI/Swiggy/pay. Avl Bal: Rs.15000.',
        'com.android.mms',
        ['LOW']
      );
    });

    it('Legitimate Utility Bill Reminder', async () => {
      await evaluate(
        'Your electricity bill of Rs.1200 is due on 15-Sep. Pay via official app to avoid late fees. BESCOM',
        'com.android.mms',
        ['LOW', 'MEDIUM'] // Allow medium if it triggers urgency
      );
    });
    
    it('Legitimate Government OTP', async () => {
      await evaluate(
        '749210 is your Aadhaar OTP for e-KYC. Valid for 10 mins. UIDAI.',
        'com.android.mms',
        ['LOW']
      );
    });
  });

  describe('Scam (MALICIOUS) Scenarios', () => {
    it('Digital Arrest Scam', async () => {
      await evaluate(
        'CBI Alert: Your Aadhaar is linked to illegal money laundering. Call immediately or face digital arrest.',
        'com.whatsapp',
        ['HIGH', 'CRITICAL']
      );
    });

    it('Telecom KYC Scam', async () => {
      await evaluate(
        'Dear Airtel User, your SIM will be blocked in 24 hrs due to pending KYC. Download AnyDesk for quick support.',
        'com.android.mms',
        ['HIGH', 'CRITICAL']
      );
    });

    it('UPI Collect Scam (Refund/Cashback)', async () => {
      await evaluate(
        'Congratulations! You won Rs 5000 cashback. Claim now upi://pay?pa=scammer@okhdfcbank&pn=Cashback&am=5000',
        'com.whatsapp',
        ['CRITICAL']
      );
    });

    it('Fake IPO/Trading Scam', async () => {
      await evaluate(
        'Guaranteed 300% returns in upcoming SME IPO! Join our premium WhatsApp group and send funds to crypto wallet.',
        'org.telegram.messenger',
        ['HIGH', 'CRITICAL']
      );
    });

    it('Electricity Disconnection Scam', async () => {
      await evaluate(
        'Dear customer, your electricity power will be disconnected at 9:30 PM tonight. Update your bill immediately. Call our officer at 9876543210.',
        'com.android.mms',
        ['CRITICAL']
      );
    });
    
    it('QR Scam', async () => {
      await evaluate(
        'Scan this QR code and enter your UPI PIN to receive your refund of Rs 10,000.',
        'com.whatsapp',
        ['CRITICAL']
      );
    });
  });
});
