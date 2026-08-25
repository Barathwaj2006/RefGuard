package com.refguard.app.api

/**
 * Wire-format models matching contracts/schemas/scan-request.json EXACTLY.
 * Field names use snake_case to match JSON contract directly.
 */
data class ScanRequestDto(
    val content_type: String,
    val content_value: String,
    val source_context: String?,
    val timestamp: String
)

/**
 * Wire-format model matching contracts/schemas/scan-response.json EXACTLY.
 */
data class ScanResponseDto(
    val scan_id: String,
    val timestamp: String,
    val risk_assessment: RiskAssessmentDto,
    val protection_decision: ProtectionDecisionDto,
    val payment_intent_mismatch: PaymentIntentMismatchDto?,
    val scam_chain: ScamChainDto?,
    val evidence_pack: EvidencePackDto?
)

/** contracts/schemas/risk-assessment.json */
data class RiskAssessmentDto(
    val risk_score: Int,
    val risk_severity: String,           // LOW | MEDIUM | HIGH | CRITICAL | UNKNOWN
    val confidence: Double,
    val signals: List<String>,
    val evidence_references: List<String>?,
    val human_explanation: String,
    val recommended_action: String
)

/** contracts/schemas/protection-decision.json */
data class ProtectionDecisionDto(
    val action: String,                  // ALLOW | WARN_CAUTION | REQUIRE_CONFIRMATION | DISCOURAGE_PROCEED
    val detected_summary: String,
    val why_it_matters: String,
    val user_instruction: String
)

/** contracts/schemas/payment-intent-mismatch.json */
data class PaymentIntentMismatchDto(
    val status: String,                  // DETECTED | NOT_DETECTED | UNKNOWN | NOT_OBSERVED
    val stated_intent: String?,
    val actual_payment_action: String?,
    val payment_direction: String,       // OUTBOUND_DEBIT | INBOUND_CREDIT | NONE | UNKNOWN
    val amount: Double?,
    val recipient_vpa: String?,
    val confidence: Double,
    val provenance: String,
    val evidence: List<String>?
)

/** contracts/schemas/scam-chain.json — Node */
data class ScamChainNodeDto(
    val node_id: String,
    val node_type: String,
    val entity_reference: String?,
    val evidence_references: List<String>?
)

/** contracts/schemas/scam-chain.json — Edge */
data class ScamChainEdgeDto(
    val from_node: String,
    val to_node: String,
    val relationship: String,
    val confidence: Double,
    val provenance: String,
    val evidence_references: List<String>?
)

/** contracts/schemas/scam-chain.json */
data class ScamChainDto(
    val nodes: List<ScamChainNodeDto>,
    val edges: List<ScamChainEdgeDto>
)

/** contracts/schemas/evidence-pack.json — Item */
data class EvidenceItemDto(
    val evidence_id: String,
    val evidence_type: String,
    val data: String
)

/** contracts/schemas/evidence-pack.json */
data class EvidencePackDto(
    val incident_id: String,
    val timestamp: String,
    val items: List<EvidenceItemDto>
)

/** contracts/schemas/scam-report.json */
data class ScamReportDto(
    val report_id: String,
    val reported_indicator: String,
    val report_category: String,
    val description: String?,
    val evidence_references: List<String>?,
    val submission_timestamp: String,
    val moderation_status: String,       // PENDING | VERIFIED | REJECTED
    val confidence: Double,
    val provenance: String
)

data class ReportResponseDto(
    val report_id: String,
    val status: String
)

data class ErrorResponseDto(
    val error_code: String,
    val message: String,
    val details: List<String>?,
    val timestamp: String
)

data class FeedbackRequestDto(
    val scan_id: String,
    val indicator: String?,
    val verdict: String, // CONFIRMED_FRAUD | FALSE_ALARM
    val user_notes: String?
)

data class FeedbackResponseDto(
    val status: String,
    val scan_id: String,
    val verdict: String,
    val message: String
)
