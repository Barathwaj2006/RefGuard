import { v4 as uuidv4 } from 'uuid';
import {
  ScanRequest,
  ScanResponse,
  ExtractionResult,
  ThreatAssessment,
  RiskAssessment,
  ProtectionDecision,
} from '../models/types';

export class AnalyzerService {
  private extractContent(request: ScanRequest): ExtractionResult {
    return {
      extracted_entities: [],
      normalized_content: request.content_value.toLowerCase(),
    };
  }

  private assessThreat(extraction: ExtractionResult): ThreatAssessment {
    const isSuspicious = extraction.normalized_content.includes('scam') || extraction.normalized_content.includes('test-threat');
    return {
      threat_detected: isSuspicious,
      threat_type: isSuspicious ? 'PHISHING' : undefined,
      confidence: isSuspicious ? 0.9 : 0.1,
    };
  }

  private assessRisk(threat: ThreatAssessment): RiskAssessment {
    if (threat.threat_detected) {
      return {
        risk_score: 90,
        risk_severity: 'HIGH',
        confidence: 0.85,
        signals: ['suspicious_keyword_match'],
        human_explanation: 'We detected suspicious patterns often associated with scams.',
        recommended_action: 'Do not proceed with the transaction or click any links.'
      };
    }
    return {
      risk_score: 10,
      risk_severity: 'LOW',
      confidence: 0.95,
      signals: [],
      human_explanation: 'No immediate threats detected.',
      recommended_action: 'Proceed with normal caution.'
    };
  }

  private makeDecision(risk: RiskAssessment): ProtectionDecision {
    if (risk.risk_severity === 'HIGH' || risk.risk_severity === 'CRITICAL') {
      return {
        action: 'DISCOURAGE_PROCEED',
        detected_summary: 'High risk content detected',
        why_it_matters: 'This looks like a known scam attempt that could result in financial loss.',
        user_instruction: 'Close this application and do not share any details.'
      };
    }
    if (risk.risk_severity === 'MEDIUM') {
      return {
        action: 'WARN_CAUTION',
        detected_summary: 'Medium risk content detected',
        why_it_matters: 'There are some suspicious signals, though it may not be a scam.',
        user_instruction: 'Verify the sender\'s identity before proceeding.'
      };
    }
    return {
      action: 'ALLOW',
      detected_summary: 'Content appears safe',
      why_it_matters: 'No known threat signatures matched.',
      user_instruction: 'None'
    };
  }

  public analyze(request: ScanRequest): ScanResponse {
    const extraction = this.extractContent(request);
    const threat = this.assessThreat(extraction);
    const risk = this.assessRisk(threat);
    const decision = this.makeDecision(risk);

    return {
      scan_id: uuidv4(),
      timestamp: new Date().toISOString(),
      risk_assessment: risk,
      protection_decision: decision,
    };
  }
}
