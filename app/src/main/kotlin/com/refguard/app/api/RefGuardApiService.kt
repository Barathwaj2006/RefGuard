package com.refguard.app.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class ScanRequestDto(
    val content_type: String,
    val content_value: String,
    val source_context: String = "MANUAL_SCAN",
    val timestamp: String? = null
)

data class RiskAssessmentDto(
    val risk_score: Int = 0,
    val risk_severity: String = "UNKNOWN",
    val confidence: Double = 0.9,
    val signals: List<String> = emptyList(),
    val evidence_references: List<String>? = null,
    val human_explanation: String = "",
    val recommended_action: String = ""
)

data class ProtectionDecisionDto(
    val action: String = "ALLOW",
    val detected_summary: String = "",
    val why_it_matters: String = "",
    val user_instruction: String = ""
)

data class PaymentIntentMismatchDto(
    val mismatch_status: String = "NOT_DETECTED",
    val stated_intent: String? = null,
    val actual_payment_action: String = "NONE",
    val payment_direction: String = "NONE",
    val amount: Double? = null,
    val recipient_vpa: String? = null
)

data class ScamChainNodeDto(
    val node_id: String = "",
    val label: String? = null,
    val node_type: String? = null,
    val description: String? = null,
    val state: String? = null,
    val confidence: Double = 0.0,
    val provenance: String? = null,
    val entity_reference: String? = null,
    val evidence_references: List<String> = emptyList(),
    val risk_weight: Double = 0.0
)

data class ScamChainEdgeDto(
    val from_node: String = "",
    val to_node: String = "",
    val relationship: String = "",
    val confidence: Double = 0.0,
    val provenance: String? = null,
    val evidence_references: List<String> = emptyList()
)

data class ScamChainDto(
    val nodes: List<ScamChainNodeDto> = emptyList(),
    val edges: List<ScamChainEdgeDto> = emptyList()
)

data class EvidenceItemDto(
    val evidence_id: String = "",
    val evidence_type: String = "",
    val data: String = "",
    val explanation: String = "",
    val source_category: String = ""
)

data class EvidencePackDto(
    val items: List<EvidenceItemDto> = emptyList()
)

data class ScanResponseDto(
    val scan_id: String = "",
    val timestamp: String = "",
    val risk_assessment: RiskAssessmentDto? = null,
    val protection_decision: ProtectionDecisionDto? = null,
    val payment_intent_mismatch: PaymentIntentMismatchDto? = null,
    val scam_chain: ScamChainDto? = null,
    val evidence_pack: EvidencePackDto? = null
)

data class ScamReportDto(
    val indicator: String,
    val scam_type: String,
    val description: String,
    val timestamp: String = java.time.Instant.now().toString()
)

data class ReportResponseDto(
    val report_id: String,
    val status: String
)

data class FeedbackRequestDto(
    val scan_id: String,
    val feedback_type: String,
    val recipient_vpa: String? = null,
    val comments: String? = null
)

data class FeedbackResponseDto(
    val status: String,
    val scan_id: String,
    val feedback_type: String,
    val message: String
)

interface RefGuardApiService {
    @POST("v1/scan")
    suspend fun scan(@Body request: ScanRequestDto): Response<ScanResponseDto>

    @POST("v1/report")
    suspend fun report(@Body report: ScamReportDto): Response<ReportResponseDto>

    @POST("v1/feedback")
    suspend fun submitFeedback(@Body feedback: FeedbackRequestDto): Response<FeedbackResponseDto>
}
