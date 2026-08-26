package com.refguard.app.domain

import com.refguard.app.api.EvidenceItemDto
import com.refguard.app.api.ScamChainNodeDto

/**
 * Human-readable mappings for security signals, threat chains, and evidence.
 * Eliminates technical snake_case identifiers and raw IDs from primary user-facing screens.
 */
object HumanMappers {

    data class SignalInfo(
        val title: String,
        val description: String
    )

    data class EvidenceDisplay(
        val title: String,
        val content: String,
        val whyItMatters: String,
        val source: String,
        val rawId: String
    )

    data class ScamStepDisplay(
        val stepNumber: Int,
        val stageName: String,
        val label: String,
        val detail: String,
        val isCritical: Boolean
    )

    fun mapSignal(signalKey: String): SignalInfo {
        return when (signalKey.lowercase().trim()) {
            "payment_intent_inversion" -> SignalInfo(
                title = "Payment-Intent Inversion Trap",
                description = "Sender claimed you would receive funds, but the generated payment triggers an outgoing debit from your bank account."
            )
            "credential_otp_harvesting" -> SignalInfo(
                title = "OTP / PIN Solicitation Attempt",
                description = "Message attempts to capture sensitive one-time passwords, UPI PINs, or banking authorization codes."
            )
            "deceptive_reward_trigger" -> SignalInfo(
                title = "Deceptive Reward or Cashback Lure",
                description = "Uses urgent promises of fake lottery winnings, utility bill refunds, or cash rewards to lower vigilance."
            )
            "suspicious_tld_domain" -> SignalInfo(
                title = "Unverified High-Risk Website Link",
                description = "Contains web links hosted on suspicious top-level domains commonly utilized in phishing campaigns."
            )
            "local_threat_blacklist_match" -> SignalInfo(
                title = "Known Fraudulent UPI Handle",
                description = "Recipient UPI handle matches verified cybercrime incident databases and active scam records."
            )
            "verified_merchant_whitelist" -> SignalInfo(
                title = "Verified Merchant Payment",
                description = "The recipient VPA belongs to a recognized, verified commercial merchant gateway."
            )
            "standard_payment_request" -> SignalInfo(
                title = "Standard Payment Request",
                description = "Standard payment structure with no detected inversion patterns or known malicious signatures."
            )
            else -> {
                val formatted = signalKey
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .split(' ')
                    .joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                SignalInfo(
                    title = formatted,
                    description = "Detected security indicator during automated payload analysis."
                )
            }
        }
    }

    fun mapNodeType(rawType: String): String {
        return when (rawType.uppercase().trim()) {
            "MESSAGE", "INGRESS_CONTENT" -> "Incoming Message"
            "UPI_REQUEST", "PAYMENT_REQUEST" -> "UPI Payment Request"
            "PAYMENT_ACTION" -> "Payment Action"
            "URL", "LINK" -> "Destination Web Link"
            "CREDENTIAL_CAPTURE" -> "Credential Solicitation"
            else -> rawType.replace('_', ' ').lowercase().capitalizeWords()
        }
    }

    fun mapEvidenceType(rawType: String): String {
        return when (rawType.uppercase().trim()) {
            "ORIGINAL_CONTENT" -> "Provided Content"
            "UPI_IDENTIFIER" -> "Target UPI Identifier"
            "URL_TARGET" -> "Extracted Web Destination"
            "MISMATCH_DATA" -> "Intent Mismatch Analysis"
            "OCR_EXTRACT" -> "Visual Text Extraction"
            else -> rawType.replace('_', ' ').lowercase().capitalizeWords()
        }
    }

    fun mapEvidenceItem(item: EvidenceItemDto): EvidenceDisplay {
        val title = mapEvidenceType(item.evidence_type)
        val why = when (item.evidence_type.uppercase().trim()) {
            "ORIGINAL_CONTENT" -> "Raw text or payment link supplied for verification."
            "UPI_IDENTIFIER" -> "The destination Virtual Payment Address that will receive funds if authorized."
            "URL_TARGET" -> "Web server to which the user was directed."
            "MISMATCH_DATA" -> "Direct contradiction between promised outcome and actual banking transaction."
            else -> item.explanation ?: "Verified artifact during threat intelligence analysis."
        }
        val source = when (item.source_category.uppercase().trim()) {
            "LOCAL_EDGE_CLASSIFIER" -> "Offline On-Device Analyzer"
            "UPI_INTENT_DECODER" -> "UPI Protocol Decoder"
            "CLOUD_THREAT_INTEL" -> "National Threat Intelligence"
            "GEMINI_SECURITY_REASONER" -> "Gemini Security Analysis"
            else -> item.source_category.replace('_', ' ').capitalizeWords()
        }

        return EvidenceDisplay(
            title = title,
            content = item.data,
            whyItMatters = why,
            source = source,
            rawId = item.evidence_id
        )
    }

    fun mapScamChainNode(node: ScamChainNodeDto, index: Int, isTotalCritical: Boolean): ScamStepDisplay {
        val stageName = mapNodeType(node.node_type)
        val entityRef = node.entity_reference ?: "Payment parameter"
        val isCrit = isTotalCritical && (node.node_type.contains("PAYMENT") || node.node_type.contains("CREDENTIAL"))

        val detail = when (node.node_type.uppercase().trim()) {
            "MESSAGE" -> "Sender sent a claim: \"$entityRef\""
            "UPI_REQUEST" -> "Prompted payment to VPA: $entityRef"
            "PAYMENT_ACTION" -> "Triggers $entityRef"
            else -> "Observed $stageName ($entityRef)"
        }

        return ScamStepDisplay(
            stepNumber = index + 1,
            stageName = stageName,
            label = entityRef,
            detail = detail,
            isCritical = isCrit
        )
    }

    private fun String.capitalizeWords(): String {
        return this.split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
