package com.refguard.app.domain

import com.refguard.app.api.EvidenceItemDto
import com.refguard.app.api.ScamChainEdgeDto
import com.refguard.app.api.ScamChainNodeDto

enum class RiskLevel {
    SAFE,
    WARNING,
    HIGH,
    CRITICAL,
    UNKNOWN
}

enum class MismatchStatus {
    DETECTED,
    NOT_DETECTED
}

enum class ProtectionAction {
    ALLOW,
    WARN_CAUTION,
    REQUIRE_CONFIRMATION,
    DISCOURAGE_PROCEED
}

data class ScanResult(
    val scanId: String,
    val timestamp: String,
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val riskConfidence: Double = 0.9,
    val signals: List<String> = emptyList(),
    val humanExplanation: String = "",
    val recommendedAction: String = "",
    val protectionAction: ProtectionAction = ProtectionAction.ALLOW,
    val detectedSummary: String = "",
    val whyItMatters: String = "",
    val userInstruction: String = "",
    val mismatchStatus: MismatchStatus = MismatchStatus.NOT_DETECTED,
    val statedIntent: String? = null,
    val actualPaymentAction: String = "NONE",
    val paymentDirection: String = "NONE",
    val mismatchAmount: Double? = null,
    val recipientVpa: String? = null,
    val scamChainNodes: List<ScamChainNodeDto> = emptyList(),
    val scamChainEdges: List<ScamChainEdgeDto> = emptyList(),
    val evidenceItems: List<EvidenceItemDto> = emptyList(),
    val isLocalEdgeResult: Boolean = false
)
