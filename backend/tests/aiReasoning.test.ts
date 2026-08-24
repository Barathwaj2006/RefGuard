import { sanitizeText } from '../src/services/extractors/piiSanitizer';
import { extractTradingFraudSignals } from '../src/services/extractors/tradingFraudExtractor';
import { analyzeWithGemini, shouldEscalateToGemini } from '../src/services/geminiReasoningService';

// Mock GoogleGenAI for timeout and error tests
jest.mock('@google/genai', () => {
  return {
    GoogleGenAI: jest.fn().mockImplementation(() => {
      return {
        models: {
          generateContent: jest.fn().mockImplementation(async (params) => {
            if (params.contents.includes('TIMEOUT_TEST')) {
              return new Promise(resolve => {
                const t = setTimeout(resolve, 9000);
                if (t && typeof t.unref === 'function') t.unref();
              });
            }
            if (params.contents.includes('API_FAILURE_TEST')) {
              throw new Error('API Error');
            }
            if (params.contents.includes('MALFORMED_OUTPUT_TEST')) {
              return { text: 'Not a JSON block' };
            }
            if (params.contents.includes('INVALID_SCORE_TEST')) {
              return { text: '```json\n{"risk_adjustment": 999, "reasoning": "bad bounds", "confidence": 1.0}\n```' };
            }
            return {
              text: '```json\n{"risk_adjustment": 10, "reasoning": "mock reasoning", "confidence": 0.8, "detected_patterns": ["mock_pattern"]}\n```'
            };
          })
        }
      };
    })
  };
});

describe('AI/ML Implementation Verification', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...originalEnv, GEMINI_API_KEY: 'mock_key' };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  describe('1. PII Sanitization', () => {
    it('should redact Aadhaar, PAN, Phone, Email, OTP', () => {
      const text = 'My Aadhaar is 1234-5678-9012, PAN ABCDE1234F, phone 9876543210, email test@example.com, and OTP is 123456.';
      const res = sanitizeText(text);
      expect(res.hadPii).toBe(true);
      expect(res.sanitizedText).not.toContain('1234-5678-9012');
      expect(res.sanitizedText).toContain('[AADHAAR_REDACTED]');
      expect(res.sanitizedText).toContain('[PAN_REDACTED]');
      expect(res.sanitizedText).toContain('XXXXXX3210');
      expect(res.sanitizedText).toContain('t***@example.com');
      expect(res.sanitizedText).toContain('[REDACTED]');
    });
  });

  describe('2. Deterministic Trading/Investment Signals', () => {
    it('should detect fake broker', () => {
      const res = extractTradingFraudSignals('Join my zerodha premium tip group for guaranteed returns');
      expect(res.fakeBrokerReference).toBe(true);
      expect(res.guaranteedReturnClaim).toBe(true);
      expect(res.tradingTipGroup).toBe(true);
      expect(res.hasTradingFraudSignals).toBe(true);
    });

    it('should detect fake IPO', () => {
      const res = extractTradingFraudSignals('Get guaranteed IPO allotment for upcoming shares.');
      expect(res.fakeIpoAllotment).toBe(true);
    });

    it('should detect KYC/Demat scam', () => {
      const res = extractTradingFraudSignals('Update your e-KYC PAN immediately or demat account will be blocked.');
      expect(res.kycRequest).toBe(true);
      expect(res.dematAccountReference).toBe(true);
    });
  });

  describe('3. Gemini Reasoning Integration', () => {
    it('should determine escalation properly', () => {
      expect(shouldEscalateToGemini(10)).toBe(false); // benign
      expect(shouldEscalateToGemini(50)).toBe(true);  // ambiguous
      expect(shouldEscalateToGemini(95)).toBe(false); // critical
    });

    it('should handle standard structured output correctly', async () => {
      const result = await analyzeWithGemini({
        sanitizedContent: 'Valid text',
        deterministicScore: 50,
        existingSignals: [],
        contentType: 'TEXT'
      });
      expect(result.gemini_used).toBe(true);
      expect(result.risk_adjustment).toBe(10);
    });

    it('should handle Gemini timeout gracefully', async () => {
      const result = await analyzeWithGemini({
        sanitizedContent: 'TIMEOUT_TEST',
        deterministicScore: 50,
        existingSignals: [],
        contentType: 'TEXT'
      });
      expect(result.gemini_used).toBe(false);
      expect(result.reasoning).toContain('timed out');
    }, 15000); // increase test timeout just in case

    it('should handle Gemini API failure gracefully', async () => {
      const result = await analyzeWithGemini({
        sanitizedContent: 'API_FAILURE_TEST',
        deterministicScore: 50,
        existingSignals: [],
        contentType: 'TEXT'
      });
      expect(result.gemini_used).toBe(false);
      expect(result.reasoning).toContain('failed');
    });

    it('should handle malformed Gemini output safely', async () => {
      const result = await analyzeWithGemini({
        sanitizedContent: 'MALFORMED_OUTPUT_TEST',
        deterministicScore: 50,
        existingSignals: [],
        contentType: 'TEXT'
      });
      expect(result.gemini_used).toBe(false); // JSON parse fails, catches error
    });

    it('should clamp invalid risk scores', async () => {
      const result = await analyzeWithGemini({
        sanitizedContent: 'INVALID_SCORE_TEST',
        deterministicScore: 50,
        existingSignals: [],
        contentType: 'TEXT'
      });
      expect(result.gemini_used).toBe(true);
      expect(result.risk_adjustment).toBe(20); // Clamped to max
    });
  });
});
