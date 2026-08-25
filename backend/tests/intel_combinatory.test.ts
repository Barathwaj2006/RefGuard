import { AnalyzerService } from '../src/services/analyzer';
import { ScanRequest } from '../src/models/types';
import { communityStore } from '../src/services/communityStore';

describe('AnalyzerService - Threat Intelligence Combinatory Scoring', () => {
  let analyzer: AnalyzerService;

  beforeAll(() => {
    analyzer = new AnalyzerService();
    // Add seed and community indicators for tests
    communityStore.addReport({
      report_id: 'test-community-1',
      reported_indicator: 'test.weak.community@ybl',
      report_category: 'SCAM',
      submission_timestamp: new Date().toISOString(),
      moderation_status: 'PENDING',
      confidence: 0.5,
      provenance: 'tester1'
    });
    for (let i = 0; i < 5; i++) {
        communityStore.addReport({
          report_id: `test-community-strong-${i}`,
          reported_indicator: 'test.strong.community@ybl',
          report_category: 'SCAM',
          submission_timestamp: new Date().toISOString(),
          moderation_status: 'PENDING',
          confidence: 0.9,
          provenance: `tester${i}`
        });
    }
  });

  const analyzeAndCheck = async (content: string, expectedSeverityIn: string[], expectedScoreMin: number) => {
    const request: ScanRequest = {
      content_type: 'TEXT',
      content_value: content,
      timestamp: new Date().toISOString()
    };
    const response = await analyzer.analyze(request);
    expect(expectedSeverityIn).toContain(response.risk_assessment.risk_severity);
    expect(response.risk_assessment.risk_score).toBeGreaterThanOrEqual(expectedScoreMin);
    return response;
  };

  it('Verified high-confidence indicator (VERIFIED_SEED)', async () => {
    const response = await analyzeAndCheck('Please pay scammer@upi for your task.', ['CRITICAL', 'HIGH'], 80);
    expect(response.risk_assessment.signals).toContain('verified_threat_intelligence_match');
  });

  it('One community report (Weak Intelligence)', async () => {
    const response = await analyzeAndCheck('Hello test.weak.community@ybl', ['MEDIUM', 'HIGH'], 30);
    expect(response.risk_assessment.signals.some((s: string) => s.includes('local_community_blacklist_match'))).toBe(true);
  });

  it('Multiple corroborating community reports (Strong Intelligence)', async () => {
    const response = await analyzeAndCheck('Hello test.strong.community@ybl', ['HIGH', 'CRITICAL'], 60);
    expect(response.risk_assessment.signals.some((s: string) => s.includes('local_community_blacklist_match'))).toBe(true);
  });

  it('Conflicting/benign contextual evidence (Legitimate Depository)', async () => {
    const response = await analyzeAndCheck('CDSL alert for test.weak.community@ybl', ['LOW'], 0);
    expect(response.risk_assessment.signals).toContain('verified_depository_alert');
  });

  it('Legitimate entity with low/no threat intelligence', async () => {
    const response = await analyzeAndCheck('Pay swiggy@icici for your order.', ['LOW'], 0);
    expect(response.risk_assessment.signals).toContain('verified_merchant_whitelist');
  });

  it('Payment-intent mismatch combined with weak intelligence', async () => {
    // Collect request + reward words + weak intelligence
    const response = await analyzeAndCheck('You won a prize! Claim here upi://pay?pa=test.weak.community@ybl&am=5000', ['CRITICAL', 'HIGH'], 80);
    expect(response.risk_assessment.signals).toContain('payment_intent_mismatch');
    expect(response.risk_assessment.signals.some((s: string) => s.includes('local_community_blacklist_match'))).toBe(true);
  });

  it('Strong intelligence combined with other evidence', async () => {
    // Collect request + reward words + STRONG intelligence -> should be CRITICAL
    const response = await analyzeAndCheck('You won a prize! Claim here upi://pay?pa=test.strong.community@ybl&am=5000', ['CRITICAL'], 100);
    expect(response.risk_assessment.signals).toContain('payment_intent_mismatch');
    expect(response.risk_assessment.signals.some((s: string) => s.includes('local_community_blacklist_match'))).toBe(true);
  });
});
