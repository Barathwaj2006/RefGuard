import { AnalyzerService } from '../../src/services/analyzer';
import { ScanRequest } from '../../src/models/types';

describe('AnalyzerService', () => {
  let analyzerService: AnalyzerService;

  beforeEach(() => {
    analyzerService = new AnalyzerService();
  });

  describe('analyze', () => {
    it('should allow safe content with LOW risk severity', () => {
      const request: ScanRequest = {
        content_type: 'TEXT',
        content_value: 'Hello, how are you today?',
        timestamp: new Date().toISOString(),
      };

      const response = analyzerService.analyze(request);

      expect(response).toBeDefined();
      expect(response.scan_id).toBeDefined();
      expect(response.risk_assessment.risk_severity).toBe('LOW');
      expect(response.protection_decision.action).toBe('ALLOW');
      expect(response.protection_decision.detected_summary).toBe('Content appears safe');
    });

    it('should discourage proceeding when content contains "scam"', () => {
      const request: ScanRequest = {
        content_type: 'TEXT',
        content_value: 'This is a scam to steal your money!',
        timestamp: new Date().toISOString(),
      };

      const response = analyzerService.analyze(request);

      expect(response).toBeDefined();
      expect(response.risk_assessment.risk_severity).toBe('HIGH');
      expect(response.risk_assessment.signals).toContain('suspicious_keyword_match');
      expect(response.protection_decision.action).toBe('DISCOURAGE_PROCEED');
      expect(response.protection_decision.detected_summary).toBe('High risk content detected');
    });

    it('should discourage proceeding when content contains "test-threat"', () => {
      const request: ScanRequest = {
        content_type: 'TEXT',
        content_value: 'Executing a test-threat payload.',
        timestamp: new Date().toISOString(),
      };

      const response = analyzerService.analyze(request);

      expect(response).toBeDefined();
      expect(response.risk_assessment.risk_severity).toBe('HIGH');
      expect(response.protection_decision.action).toBe('DISCOURAGE_PROCEED');
    });

    it('should be case-insensitive for threat detection', () => {
      const request: ScanRequest = {
        content_type: 'TEXT',
        content_value: 'THIS IS A SCAM!',
        timestamp: new Date().toISOString(),
      };

      const response = analyzerService.analyze(request);

      expect(response).toBeDefined();
      expect(response.risk_assessment.risk_severity).toBe('HIGH');
      expect(response.protection_decision.action).toBe('DISCOURAGE_PROCEED');
    });
  });
});
