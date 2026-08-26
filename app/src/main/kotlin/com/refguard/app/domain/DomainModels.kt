package com.refguard.app.domain

import com.refguard.app.api.*
import com.refguard.platform.models.ContentType
import com.refguard.platform.models.ScanRequest

// ──────────────────────────────────────────────
// Risk Level (maps from risk_severity contract field)
// ──────────────────────────────────────────────
enum class RiskLevel {
    SAFE,       // LOW
    WARNING,    // MEDIUM
    HIGH,       // HIGH
    CRITICAL,   // CRITICAL
    UNKNOWN
}

// ──────────────────────────────────────────────
// Protection Action (maps from protection_decision.action)
// ──────────────────────────────────────────────
enum class ProtectionAction {
    ALLOW,
    WARN_CAUTION,
    REQUIRE_CONFIRMATION,
    DISCOURAGE_PROCEED
}

// ──────────────────────────────────────────────
// Mismatch Status
// ──────────────────────────────────────────────
enum class MismatchStatus {
    DETECTED, NOT_DETECTED, UNKNOWN, NOT_OBSERVED
}

// ──────────────────────────────────────────────
// Domain ScanResult — flattened view for UI
// ──────────────────────────────────────────────
data class ScanResult(
    val scanId: String,
    val timestamp: String,

    // Risk
    val riskLevel: RiskLevel,
    val riskScore: Int,                 // 0–100
    val riskConfidence: Double,
    val signals: List<String>,
    val humanExplanation: String,
    val recommendedAction: String,

    // Protection
    val protectionAction: ProtectionAction,
    val detectedSummary: String,
    val whyItMatters: String,
    val userInstruction: String,

    // Payment mismatch (optional)
    val mismatchStatus: MismatchStatus?,
    val statedIntent: String?,
    val actualPaymentAction: String?,
    val paymentDirection: String?,
    val mismatchAmount: Double?,
    val recipientVpa: String?,

    // Scam chain
    val scamChainNodes: List<ScamChainNodeDto>,
    val scamChainEdges: List<ScamChainEdgeDto>,

    // Evidence
    val evidenceItems: List<EvidenceItemDto>,

    // Edge vs Cloud Indicator
    val isLocalEdgeResult: Boolean = false
)

// ──────────────────────────────────────────────
// Mappers: Contract DTO → Domain
// ──────────────────────────────────────────────
fun ScanResponseDto.toDomain(): ScanResult {
    val riskLevel = when (risk_assessment.risk_severity.uppercase()) {
        "LOW" -> RiskLevel.SAFE
        "MEDIUM" -> RiskLevel.WARNING
        "HIGH" -> RiskLevel.HIGH
        "CRITICAL" -> RiskLevel.CRITICAL
        else -> RiskLevel.UNKNOWN
    }

    val protectionAction = when (protection_decision.action.uppercase()) {
        "ALLOW" -> ProtectionAction.ALLOW
        "WARN_CAUTION" -> ProtectionAction.WARN_CAUTION
        "REQUIRE_CONFIRMATION" -> ProtectionAction.REQUIRE_CONFIRMATION
        "DISCOURAGE_PROCEED" -> ProtectionAction.DISCOURAGE_PROCEED
        else -> ProtectionAction.WARN_CAUTION
    }

    val mismatchStatus = payment_intent_mismatch?.status?.uppercase()?.let {
        when (it) {
            "DETECTED" -> MismatchStatus.DETECTED
            "NOT_DETECTED" -> MismatchStatus.NOT_DETECTED
            "NOT_OBSERVED" -> MismatchStatus.NOT_OBSERVED
            else -> MismatchStatus.UNKNOWN
        }
    }

    return ScanResult(
        scanId = scan_id,
        timestamp = timestamp,

        riskLevel = riskLevel,
        riskScore = risk_assessment.risk_score,
        riskConfidence = risk_assessment.confidence,
        signals = risk_assessment.signals,
        humanExplanation = risk_assessment.human_explanation,
        recommendedAction = risk_assessment.recommended_action,

        protectionAction = protectionAction,
        detectedSummary = protection_decision.detected_summary,
        whyItMatters = protection_decision.why_it_matters,
        userInstruction = protection_decision.user_instruction,

        mismatchStatus = mismatchStatus,
        statedIntent = payment_intent_mismatch?.stated_intent,
        actualPaymentAction = payment_intent_mismatch?.actual_payment_action,
        paymentDirection = payment_intent_mismatch?.payment_direction,
        mismatchAmount = payment_intent_mismatch?.amount,
        recipientVpa = payment_intent_mismatch?.recipient_vpa,

        scamChainNodes = scam_chain?.nodes ?: emptyList(),
        scamChainEdges = scam_chain?.edges ?: emptyList(),

        evidenceItems = evidence_pack?.items ?: emptyList(),
        isLocalEdgeResult = false
    )
}

// ──────────────────────────────────────────────
// Platform ScanRequest → API DTO
// ──────────────────────────────────────────────
fun ScanRequest.toDto(): com.refguard.app.api.ScanRequestDto {
    return com.refguard.app.api.ScanRequestDto(
        content_type = contentType.toContractString(),
        content_value = contentValue,
        source_context = sourceContext.takeIf { it.isNotBlank() },
        timestamp = timestamp
    )
}

fun ContentType.toContractString(): String = when (this) {
    ContentType.TEXT -> "TEXT"
    ContentType.URL -> "URL"
    ContentType.UPI_VPA -> "UPI_VPA"
    ContentType.IMAGE -> "IMAGE"
    ContentType.QR -> "QR"
    ContentType.SHARE_INTENT -> "SHARE_INTENT"
    ContentType.CLIPBOARD -> "CLIPBOARD"
    ContentType.MANUAL -> "MANUAL"
}
