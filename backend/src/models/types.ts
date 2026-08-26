export interface ScanRequest {
  content_type: 'TEXT' | 'URL' | 'UPI_VPA' | 'IMAGE' | 'QR' | 'SHARE_INTENT' | 'CLIPBOARD' | 'MANUAL';
  content_value: string;
  source_context?: string;
  timestamp: string;
}

export interface RiskAssessment {
  risk_score: number;
  risk_severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | 'UNKNOWN';
  confidence: number;
  signals: string[];
  evidence_references?: string[];
  human_explanation: string;
  recommended_action: string;
}

export interface ProtectionDecision {
  action: 'ALLOW' | 'WARN_CAUTION' | 'REQUIRE_CONFIRMATION' | 'DISCOURAGE_PROCEED';
  detected_summary: string;
  why_it_matters: string;
  user_instruction: string;
}

export interface PaymentIntentMismatch {
  status: 'DETECTED' | 'NOT_DETECTED' | 'UNKNOWN' | 'NOT_OBSERVED';
  stated_intent?: string;
  actual_payment_action?: string;
  payment_direction: 'OUTBOUND_DEBIT' | 'INBOUND_CREDIT' | 'NONE' | 'UNKNOWN';
  amount?: number;
  recipient_vpa?: string;
  confidence: number;
  provenance: string;
  evidence?: string[];
}

export interface ScamChain {
  nodes: Array<{
    node_id: string;
    node_type: 'MESSAGE' | 'REFERRAL' | 'SHORT_LINK' | 'REDIRECT' | 'LANDING_PAGE' | 'UPI_REQUEST' | 'PAYMENT_ACTION' | 'ACCOUNT_TAKEOVER';
    state: 'OBSERVED' | 'INFERRED' | 'PREDICTED';
    confidence: number;
    provenance: string;
    entity_reference?: string;
    evidence_references?: string[];
  }>;
  edges: Array<{
    from_node: string;
    to_node: string;
    relationship: string;
    confidence: number;
    provenance: string;
    evidence_references?: string[];
  }>;
}

export interface EvidencePack {
  incident_id: string;
  timestamp: string;
  items: Array<{
    evidence_id: string;
    evidence_type: 'ORIGINAL_CONTENT' | 'EXTRACTED_ENTITY' | 'URL' | 'UPI_IDENTIFIER' | 'RISK_SIGNAL';
    data: string;
    explanation?: string;
    source_category?: string;
  }>;
}

export interface AdaptiveScamIntelligence {
  archetype: string;
  current_stage: string;
  stage_title: string;
  stage_index: number;
  total_stages: number;
  stages_sequence: string[];
  observed_evidence: string[];
  inferred_intent: string[];
  predicted_next_steps: string[];
  previous_likely_stage: string | null;
  next_likely_stage: string | null;
  next_likely_step: string | null;
  attacker_objective: string;
  user_risk: string;
  recommended_action: string;
  confidence: number;
  reporting_path: string;
  evidence_backed_status: boolean;
  provenance: string;
}

export interface ScanResponse {
  scan_id: string;
  timestamp: string;
  risk_assessment: RiskAssessment;
  protection_decision: ProtectionDecision;
  payment_intent_mismatch?: PaymentIntentMismatch;
  scam_chain?: ScamChain;
  evidence_pack?: EvidencePack;
  adaptive_scam_intelligence?: AdaptiveScamIntelligence;
}

export interface ScamReport {
  report_id: string;
  reported_indicator: string;
  report_category: string;
  description?: string;
  evidence_references?: string[];
  submission_timestamp: string;
  moderation_status: 'PENDING' | 'VERIFIED' | 'REJECTED';
  confidence: number;
  provenance: string;
}

export interface ErrorResponse {
  error_code: string;
  error_message: string;
  details?: string;
}

// Internal engine placeholder interfaces
export interface ExtractionResult {
  extracted_entities: string[];
  normalized_content: string;
}

export interface ThreatAssessment {
  threat_detected: boolean;
  threat_type?: string;
  confidence: number;
}
