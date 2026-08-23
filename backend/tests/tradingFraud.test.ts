import { extractTradingFraudSignals } from '../src/services/extractors/tradingFraudExtractor';

describe('Trading Fraud Extractor', () => {
  it('should detect SEBI reference', () => {
    const text = 'I am a SEBI registered investment advisor with INZ000000000. Join my premium channel.';
    const result = extractTradingFraudSignals(text);
    expect(result.sebiReference).toBe(true);
    expect(result.hasTradingFraudSignals).toBe(true);
    expect(result.matchedKeywords).toContain('sebi_reference');
  });

  it('should detect fake broker reference', () => {
    const text = 'Open free Demat in Zerodha and get guaranteed profit tips.';
    const result = extractTradingFraudSignals(text);
    expect(result.fakeBrokerReference).toBe(true);
    expect(result.detectedBrokerNames).toContain('zerodha');
  });

  it('should detect broker app links', () => {
    const text = 'Download this amazing trading app to double your money: http://fake-broker-app.com';
    const result = extractTradingFraudSignals(text);
    expect(result.brokerAppLink).toBe(true);
  });

  it('should detect crypto wallet addresses', () => {
    const text = 'Deposit minimum $100 to this wallet 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa to activate your account.';
    const result = extractTradingFraudSignals(text);
    expect(result.cryptoWalletAddress).toBe(true);
    expect(result.detectedCryptoAddresses.length).toBeGreaterThan(0);
  });

  it('should detect guaranteed return claims', () => {
    const text = 'Get 50% guaranteed returns weekly without any risk.';
    const result = extractTradingFraudSignals(text);
    expect(result.guaranteedReturnClaim).toBe(true);
  });

  it('should detect IPO scam patterns', () => {
    const text = 'Buy pre-IPO shares of Swiggy now before it lists! Guaranteed allotment.';
    const result = extractTradingFraudSignals(text);
    expect(result.fakeIpoAllotment).toBe(true);
  });

  it('should detect demat account solicitation', () => {
    const text = 'Send your demat account number to receive the bonus.';
    const result = extractTradingFraudSignals(text);
    expect(result.dematAccountReference).toBe(true);
  });

  it('should detect trading tip groups', () => {
    const text = 'Subscribe to our premium stock tips telegram group for daily jackpot calls.';
    const result = extractTradingFraudSignals(text);
    expect(result.tradingTipGroup).toBe(true);
  });

  it('should detect KYC phishing', () => {
    const text = 'Your KYC is pending. Please upload your PAN card and Aadhaar to reactivate your trading a/c.';
    const result = extractTradingFraudSignals(text);
    expect(result.kycRequest).toBe(true);
  });

  it('should detect deposit requests', () => {
    const text = 'Please transfer rs 5000 to start trading with us.';
    const result = extractTradingFraudSignals(text);
    expect(result.depositPaymentRequest).toBe(true);
  });

  it('should ignore benign text without trading fraud signals', () => {
    const text = 'Hey man, what time are we meeting for dinner? Are we still going to that pizza place?';
    const result = extractTradingFraudSignals(text);
    expect(result.hasTradingFraudSignals).toBe(false);
    expect(result.signalCount).toBe(0);
  });
});
