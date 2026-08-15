import { AnalyzerService } from '../src/services/analyzer';

describe('AnalyzerService', () => {
  let analyzer: AnalyzerService;

  beforeEach(() => {
    analyzer = new AnalyzerService();
  });

  it('should hit MEDIUM risk branch by calling makeDecision explicitly if possible, or bypass', () => {
    // makeDecision is private, so we bypass TS checks
    const decision = (analyzer as any).makeDecision({ risk_severity: 'MEDIUM' });
    expect(decision.action).toBe('WARN_CAUTION');
  });

  it('should hit CRITICAL risk branch by calling makeDecision explicitly', () => {
    // CRITICAL also shares branch with HIGH, but we can hit it explicitly
    const decision = (analyzer as any).makeDecision({ risk_severity: 'CRITICAL' });
    expect(decision.action).toBe('DISCOURAGE_PROCEED');
  });
});
