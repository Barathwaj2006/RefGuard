import { shouldEscalateToGemini, analyzeWithGemini } from '../src/services/geminiReasoningService';

// We just test the deterministic parts and the fallback behavior
describe('Gemini Reasoning Service', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...originalEnv };
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('should escalate for scores 40 and above', () => {
    expect(shouldEscalateToGemini(39)).toBe(false);
    expect(shouldEscalateToGemini(40)).toBe(true);
    expect(shouldEscalateToGemini(60)).toBe(true);
    expect(shouldEscalateToGemini(80)).toBe(true);
    expect(shouldEscalateToGemini(81)).toBe(true);
    expect(shouldEscalateToGemini(100)).toBe(true);
  });

  it('should fallback gracefully when GEMINI_API_KEY is not set', async () => {
    delete process.env.GEMINI_API_KEY;

    const result = await analyzeWithGemini({
      sanitizedContent: 'Some suspicious text',
      deterministicScore: 50,
      existingSignals: [],
      contentType: 'TEXT'
    });

    expect(result.gemini_used).toBe(false);
    expect(result.risk_adjustment).toBe(0);
    expect(result.confidence).toBe(0);
    expect(result.reasoning).toContain('Gemini reasoning unavailable');
  });

  it('should handle timeout or error by falling back gracefully', async () => {
    process.env.GEMINI_API_KEY = 'fake_key';

    // It will attempt to call Gemini and fail because of fake key (or timeout in a mock)
    // We expect it not to crash and instead return the fallback.
    const result = await analyzeWithGemini({
      sanitizedContent: 'Some suspicious text',
      deterministicScore: 50,
      existingSignals: [],
      contentType: 'TEXT'
    });

    // Since we are not actually mocking GoogleGenAI network requests here, it will throw an API key error inside analyzeWithGemini, which gets caught.
    expect(result.gemini_used).toBe(false);
    expect(result.risk_adjustment).toBe(0);
    expect(result.reasoning).toContain('Gemini reasoning failed');
  }, 10000);
});
