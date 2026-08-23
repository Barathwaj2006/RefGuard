import { extractUpiFraudSignals } from '../src/services/extractors/upiFraudExtractor';

describe('UPI/Telecom Fraud Extractor', () => {
  it('should detect digital arrest scams', () => {
    const text = 'This is CBI officer Ramesh. Your Aadhar card is linked to money laundering. We are issuing an arrest warrant. Join Skype video call for statement.';
    const result = extractUpiFraudSignals(text);
    
    expect(result.hasUpiFraudSignals).toBe(true);
    expect(result.digitalArrestScam).toBe(true);
    expect(result.matchedKeywords).toContain('digital_arrest_scam');
  });

  it('should detect electricity bill scams (Hindi/English)', () => {
    const text = 'Dear Consumer, your electricity power will be disconnected tonight at 9 PM. Update previous month bill immediately by calling electricity officer.';
    const result = extractUpiFraudSignals(text);

    expect(result.hasUpiFraudSignals).toBe(true);
    expect(result.electricityBillScam).toBe(true);
    expect(result.matchedKeywords).toContain('electricity_bill_scam');
  });

  it('should detect FedEx/Customs scams', () => {
    const text = 'Your FedEx parcel has been seized by customs because narcotics were found inside. Pay clearance fee immediately.';
    const result = extractUpiFraudSignals(text);

    expect(result.hasUpiFraudSignals).toBe(true);
    expect(result.customsParcelScam).toBe(true);
    expect(result.matchedKeywords).toContain('customs_courier_scam');
  });

  it('should detect Telecom KYC scams', () => {
    const text = 'Your Jio SIM will be blocked in 24 hours. Complete your KYC update within 10 minutes by clicking this link.';
    const result = extractUpiFraudSignals(text);

    expect(result.hasUpiFraudSignals).toBe(true);
    expect(result.telecomKycScam).toBe(true);
    expect(result.matchedKeywords).toContain('telecom_kyc_scam');
  });

  it('should detect Hinglish refund/cashback scams', () => {
    const text = 'Congratulations, aapko 5000 rs ka cashback mila hai. Paise paane ke liye PIN dalein aur scan karein.';
    const result = extractUpiFraudSignals(text);

    expect(result.hasUpiFraudSignals).toBe(true);
    expect(result.refundCashbackScam).toBe(true);
    expect(result.matchedKeywords).toContain('hinglish_cashback_scam');
  });

  it('should not flag generic benign messages', () => {
    const text = 'Hey, what time are we meeting for dinner today? I can pick you up.';
    const result = extractUpiFraudSignals(text);

    expect(result.hasUpiFraudSignals).toBe(false);
    expect(result.signalCount).toBe(0);
  });
});
