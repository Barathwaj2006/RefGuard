import { AnalyzerService } from '../src/services/analyzer';
import { extractTradingFraudSignals } from '../src/services/extractors/tradingFraudExtractor';
import { sanitizeText } from '../src/services/extractors/piiSanitizer';
import { analyzeWithGemini, GeminiEscalationInput } from '../src/services/geminiReasoningService';

// Mock the Gemini SDK to simulate timeouts and failures for some tests
jest.mock('@google/genai', () => {
  return {
    GoogleGenAI: jest.fn().mockImplementation(() => {
      return {
        models: {
          generateContent: jest.fn().mockImplementation(async (args) => {
            const prompt = args.contents.toLowerCase();
            
            if (prompt.includes('timeout_test')) {
              // Simulate timeout by never resolving (or taking very long)
              return new Promise(resolve => {
                const t = setTimeout(resolve, 10000);
                if (t && typeof t.unref === 'function') t.unref();
              });
            }
            if (prompt.includes('api_failure')) {
              throw new Error('API Rate Limit Exceeded');
            }
            if (prompt.includes('malformed_json')) {
              return { text: 'This is not JSON!' };
            }
            if (prompt.includes('invalid_score')) {
              return { text: '```json\n{"risk_adjustment": 999, "reasoning": "bad score", "confidence": 1.5, "detected_patterns": ["x"]}\n```' };
            }
            
            // Default mock success response
            return {
              text: '```json\n{"risk_adjustment": 15, "reasoning": "Looks highly suspicious based on patterns.", "confidence": 0.9, "detected_patterns": ["urgency", "fake_authority"]}\n```'
            };
          })
        }
      };
    })
  };
});

describe('AI/ML Reasoning Layer Evaluation Fixtures', () => {
  const analyzer = new AnalyzerService();

  beforeEach(() => {
    // Set a dummy API key to bypass the 'missing key' fallback in the Gemini service
    process.env.GEMINI_API_KEY = 'dummy-key';
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  // 1. Benign message
  it('should correctly classify a benign message', async () => {
    const req = { content_id: '1', content_type: 'TEXT' as const, content_value: 'Hey, are we still meeting for lunch at 1 PM?', timestamp: new Date().toISOString(), sender_id: '123' };
    const result = await analyzer.analyze(req);
    expect(result.risk_assessment.risk_severity).toBe('LOW');
  });

  // 2. UPI scam
  it('should detect a UPI collect request scam', async () => {
    const req = { content_id: '2', content_type: 'TEXT' as const, content_value: 'Dear customer, your refund is ready. Click upi://pay?pa=scammer@ybl&pn=Scam&am=5000 to collect your money by entering PIN.', timestamp: new Date().toISOString(), sender_id: '123' };
    const result = await analyzer.analyze(req);
    
    expect(result.payment_intent_mismatch).toBeDefined();
    expect(result.payment_intent_mismatch?.status).toBe('DETECTED');
    expect(result.risk_assessment.risk_severity).toBe('CRITICAL');
  });

  // 3. Trading scam & 4. Fake broker
  it('should detect trading scam and fake broker', async () => {
    const req = { content_id: '3', content_type: 'TEXT' as const, content_value: 'Join our trading group for guaranteed 50% daily returns. Download the Zerodha options app here: http://fake-zerodha.com' };
    const signals = extractTradingFraudSignals(req.content_value);
    
    expect(signals.hasTradingFraudSignals).toBe(true);
    expect(signals.fakeBrokerReference).toBe(true);
    expect(signals.tradingTipGroup).toBe(true);
  });

  // 5. Guaranteed-return scam
  it('should detect guaranteed-return claims', async () => {
    const req = { content_id: '4', content_type: 'TEXT' as const, content_value: 'No risk, assured monthly income of Rs 50,000 on your investment.' };
    const signals = extractTradingFraudSignals(req.content_value);
    expect(signals.guaranteedReturnClaim).toBe(true);
  });

  // 6. Fake IPO
  it('should detect fake IPO allotment scams', async () => {
    const req = { content_id: '5', content_type: 'TEXT' as const, content_value: 'Your pre-IPO shares for Tata Technologies are confirmed. Pay now to secure allotment.' };
    const signals = extractTradingFraudSignals(req.content_value);
    expect(signals.fakeIpoAllotment).toBe(true);
  });

  // 7. KYC / Demat scam
  it('should detect KYC/Demat phishing', async () => {
    const req = { content_id: '6', content_type: 'TEXT' as const, content_value: 'Open a demat account to avoid suspension. Update your KYC immediately at http://kyc-update.com' };
    const signals = extractTradingFraudSignals(req.content_value);
    expect(signals.kycRequest).toBe(true);
    expect(signals.dematAccountReference).toBe(true);
  });

  // 8. Multilingual / vernacular example
  it('should sanitize PII from multilingual/vernacular text', () => {
    const text = 'Bhai, mera phone number 9876543210 hai, call kar mujhe.';
    const sanitized = sanitizeText(text);
    expect(sanitized.sanitizedText).not.toContain('9876543210');
    expect(sanitized.sanitizedText).toContain('XXXXXX');
  });

  // Gemini specific tests
  
  // 9. Malformed Gemini output
  it('should safely fallback when Gemini returns malformed json', async () => {
    const input: GeminiEscalationInput = {
      sanitizedContent: 'malformed_json',
      deterministicScore: 50,
      existingSignals: [],
      contentType: 'SMS'
    };
    const verdict = await analyzeWithGemini(input);
    expect(verdict.gemini_used).toBe(false);
    expect(verdict.risk_adjustment).toBe(0);
  });

  // 10. Gemini timeout
  it('should safely fallback when Gemini API times out', async () => {
    const input: GeminiEscalationInput = {
      sanitizedContent: 'timeout_test',
      deterministicScore: 50,
      existingSignals: [],
      contentType: 'SMS'
    };
    // The test mock takes 10s, but the service times out at 8s.
    // In jest, this might exceed the default 5s test timeout, so we increase it or rely on mock timers.
    // But since the service does a setTimeout race, we can just run it. 
    // To speed up the test, we could mock setTimeout, but let's just let it run if it's 8s.
    // Actually, letting it run for 8s in unit tests is bad practice. We will rely on the service logic.
    // But we'll just mock the race or the timeout inside the service? 
    // It's better to just ensure the fallback is hit.
    const verdict = await analyzeWithGemini(input);
    expect(verdict.gemini_used).toBe(false);
    expect(verdict.reasoning).toContain('timed out');
  }, 10000);

  // 11. Gemini API failure
  it('should safely fallback on API failure', async () => {
    const input: GeminiEscalationInput = {
      sanitizedContent: 'api_failure',
      deterministicScore: 50,
      existingSignals: [],
      contentType: 'SMS'
    };
    const verdict = await analyzeWithGemini(input);
    expect(verdict.gemini_used).toBe(false);
    expect(verdict.reasoning).toContain('failed');
  });

  // 12. Invalid risk score
  it('should normalize invalid risk scores from Gemini', async () => {
    const input: GeminiEscalationInput = {
      sanitizedContent: 'invalid_score',
      deterministicScore: 50,
      existingSignals: [],
      contentType: 'SMS'
    };
    const verdict = await analyzeWithGemini(input);
    // Max adjustment is 20, so 999 should be clamped to 20
    expect(verdict.gemini_used).toBe(true);
    expect(verdict.risk_adjustment).toBe(20);
    expect(verdict.confidence).toBe(1); // 1.5 clamped to 1
  });
});
