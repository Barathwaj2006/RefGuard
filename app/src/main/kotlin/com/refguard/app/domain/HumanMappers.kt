package com.refguard.app.domain

import com.refguard.app.api.EvidenceItemDto
import com.refguard.app.api.ScamChainNodeDto

/**
 * Human-readable mappings for security signals, threat chains, and evidence.
 * Eliminates technical snake_case identifiers, raw IDs, and AI jargon from user-facing UI.
 */
object HumanMappers {

    data class SignalInfo(
        val title: String,
        val description: String
    )

    data class EvidenceDisplay(
        val whatWeFound: String,
        val whyItMatters: String,
        val source: String,
        val rawId: String
    )

    data class ScamStepDisplay(
        val stepNumber: Int,
        val stageName: String,
        val primaryText: String,
        val subText: String,
        val isCritical: Boolean
    )

    fun mapSignal(signalKey: String): SignalInfo {
        return when (signalKey.lowercase().trim()) {
            "payment_intent_inversion" -> SignalInfo(
                title = "Payment-Intent Mismatch",
                description = "You were told you would receive money, but this payment actually debits money from your account."
            )
            "deceptive_reward_trigger" -> SignalInfo(
                title = "Fake Reward / Cashback Lure",
                description = "Uses urgent promises of fake lottery winnings, utility refunds, or cashback to lower your guard."
            )
            "urgency_disconnection_threat" -> SignalInfo(
                title = "Artificial Urgency & Cutoff Panic",
                description = "Threatens imminent utility disconnection (e.g. power cutoff tonight) to pressure an immediate payment."
            )
            "unauthorized_utility_vpa" -> SignalInfo(
                title = "Unverified Personal UPI Handle",
                description = "Electricity and utility bills are collected via authorized BBPS billers, not personal @axisbank or @paytm UPI IDs."
            )
            "credential_otp_harvesting" -> SignalInfo(
                title = "OTP / PIN Solicitation Attempt",
                description = "Attempts to capture your one-time passwords, UPI PINs, or banking authorization codes."
            )
            "unrealistic_task_income_lure" -> SignalInfo(
                title = "Unrealistic Task / Work-From-Home Scam",
                description = "Promises high daily income for simple tasks (video likes) to lure you into a Ponzi advance-fee trap."
            )
            "ponzi_deposit_solicitation" -> SignalInfo(
                title = "Advance Deposit Demand",
                description = "Demands an upfront 'activation' or 'security deposit' before releasing promised earnings."
            )
            "external_channel_redirect" -> SignalInfo(
                title = "Unmonitored Channel Redirect",
                description = "Directs conversation to external Telegram/WhatsApp channels to bypass platform fraud filters."
            )
            "suspicious_tld_domain" -> SignalInfo(
                title = "Unverified Phishing Link",
                description = "Contains web links hosted on suspicious top-level domains commonly used in phishing attacks."
            )
            "phishing_parcel_lure" -> SignalInfo(
                title = "Fake Courier / Delivery Hold",
                description = "Falsely claims an India Post or courier package is on hold to trick you into clicking phishing links."
            )
            "fake_support_impersonation" -> SignalInfo(
                title = "Customer Support Impersonation",
                description = "Posing as bank/app customer support, tricking you into scanning a 'Refund QR' that debits money."
            )
            "local_threat_blacklist_match" -> SignalInfo(
                title = "Known Fraudulent UPI Handle",
                description = "Recipient UPI handle matches verified cybercrime incident databases and active fraud records."
            )
            "verified_merchant_whitelist" -> SignalInfo(
                title = "Verified Merchant Payment",
                description = "The recipient belongs to a recognized, verified commercial merchant gateway."
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
            "MESSAGE", "INGRESS_CONTENT" -> "MESSAGE"
            "UPI_REQUEST", "PAYMENT_REQUEST" -> "UPI REQUEST"
            "PAYMENT_ACTION" -> "PAYMENT ACTION"
            "URL", "LINK" -> "DESTINATION LINK"
            "CREDENTIAL_CAPTURE" -> "CREDENTIAL HARVESTING"
            else -> rawType.replace('_', ' ').uppercase()
        }
    }

    fun mapEvidenceItem(item: EvidenceItemDto): EvidenceDisplay {
        val (what, why) = when (item.evidence_type.uppercase().trim()) {
            "ORIGINAL_CONTENT" -> Pair(
                item.data,
                "The suspicious message or text received prior to transaction."
            )
            "UPI_IDENTIFIER" -> Pair(
                "Payment address: ${item.data}",
                "The destination UPI handle that will receive funds if authorized."
            )
            "URL_TARGET" -> Pair(
                "Web address: ${item.data}",
                "The external web page where the victim is directed."
            )
            "MISMATCH_DATA" -> Pair(
                "Payment Direction: ${item.data}",
                "Direct contradiction between the stated claim and the actual banking debit."
            )
            "OCR_EXTRACT" -> Pair(
                "Extracted text: ${item.data}",
                "Text captured from the uploaded image or screenshot."
            )
            else -> Pair(
                item.data,
                item.explanation ?: "Security artifact verified during threat analysis."
            )
        }

        val source = when (item.source_category.uppercase().trim()) {
            "LOCAL_EDGE_CLASSIFIER" -> "Offline Analysis"
            "UPI_INTENT_DECODER" -> "UPI Protocol Decoder"
            "CLOUD_THREAT_INTEL" -> "National Threat Intelligence"
            "GEMINI_SECURITY_REASONER" -> "Security Reasoner"
            else -> item.source_category.replace('_', ' ').lowercase().capitalizeWords()
        }

        return EvidenceDisplay(
            whatWeFound = what,
            whyItMatters = why,
            source = source,
            rawId = item.evidence_id
        )
    }

    fun mapScamChainNode(node: ScamChainNodeDto, index: Int, isTotalCritical: Boolean): ScamStepDisplay {
        val stageName = mapNodeType(node.node_type)
        val entityRef = node.entity_reference ?: "Payment request"
        val isCrit = isTotalCritical && (node.node_type.contains("PAYMENT") || node.node_type.contains("CREDENTIAL"))

        val (primary, sub) = when (node.node_type.uppercase().trim()) {
            "MESSAGE" -> Pair(
                "\"$entityRef\"",
                "Initial hook used to attract or panic the victim"
            )
            "UPI_REQUEST" -> Pair(
                entityRef,
                "Target UPI address receiving the funds"
            )
            "PAYMENT_ACTION" -> Pair(
                entityRef,
                if (entityRef.contains("Debit", true)) "Outgoing debit from your bank account" else "Transaction execution"
            )
            "URL", "LINK" -> Pair(
                entityRef,
                "External website link"
            )
            else -> Pair(
                entityRef,
                "Observed in attack chain"
            )
        }

        return ScamStepDisplay(
            stepNumber = index + 1,
            stageName = stageName,
            primaryText = primary,
            subText = sub,
            isCritical = isCrit
        )
    }

    fun formatEnumName(raw: String): String {
        return raw.replace('_', ' ')
            .lowercase()
            .split(' ')
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun String.capitalizeWords(): String {
        return this.split(' ').joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
