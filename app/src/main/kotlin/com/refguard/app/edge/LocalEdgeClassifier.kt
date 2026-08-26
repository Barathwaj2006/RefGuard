package com.refguard.app.edge

import com.refguard.app.api.EvidenceItemDto
import com.refguard.app.api.ScamChainNodeDto
import com.refguard.app.domain.*
import com.refguard.platform.decoder.UpiIntentDecoder
import com.refguard.platform.models.ScanRequest
import java.time.Instant
import java.util.UUID

object LocalEdgeClassifier {

    private val SUSPICIOUS_DOMAINS = listOf(".tk", ".xyz", ".top", ".work", ".click", ".gq", ".ml", ".cf", "indiapost-parcel-kyc", "t.me")
    private val KNOWN_MALICIOUS_VPAS = listOf(
        "rewards.claim.hub@paytm",
        "powerbill.discom@axisbank",
        "taskverify@okaxis",
        "postcharge@ibl",
        "support.refund.desk@yesbank",
        "fraudulent-collect@ybl",
        "fake-cashback-reward@paytm",
        "lottery-prize-winner@upi",
        "scammer@upi"
    )

    fun classify(request: ScanRequest): ScanResult {
        val text = request.contentValue.lowercase()
        val upiPayload = UpiIntentDecoder.decode(request.contentValue)
        val payeeVpa: String? = upiPayload?.payeeVpa
        val amount: Double? = upiPayload?.amount
        val isDebit: Boolean = upiPayload?.isCollectOrDebit == true
        val statedIntent: String? = upiPayload?.statedIntentSummary

        val scanId = "local_" + UUID.randomUUID().toString().take(8)
        val timestamp = Instant.now().toString()

        val isElectricityScam = text.contains("electricity") || text.contains("discom") || text.contains("power will be disconnected")
        val isCashbackTrap = (text.contains("cashback") || text.contains("reward") || text.contains("claim")) && isDebit && !text.contains("swiggy")
        val isTaskScam = (text.contains("task") || text.contains("youtube") || text.contains("daily from home") || text.contains("telegram"))
        val isPostalScam = (text.contains("india post") || text.contains("parcel") || text.contains("redelivery") || text.contains("house address"))
        val isCustomerCareScam = (text.contains("customer care") || text.contains("helpdesk") || text.contains("refund") || text.contains("support")) && isDebit && !text.contains("swiggy")
        val hasOtpSolicitation = (text.contains("otp") || text.contains("pin")) && 
                                (text.contains("share") || text.contains("enter") || text.contains("send") || text.contains("verify"))
        val hasSuspiciousLink = SUSPICIOUS_DOMAINS.any { text.contains(it) } || text.contains("http://") || text.contains("https://")
        val isKnownBlacklist = payeeVpa?.let { vpa -> KNOWN_MALICIOUS_VPAS.any { vpa.contains(it, ignoreCase = true) } } ?: false
        val isMerchantWhitelist = payeeVpa?.let { vpa -> listOf("swiggy@", "zomato@", "amazon@", "flipkart@", "uber@").any { vpa.contains(it, ignoreCase = true) } } ?: false

        val isMismatch = isCashbackTrap || isElectricityScam || isCustomerCareScam || (upiPayload?.hasIntentInversion == true)

        val riskLevel: RiskLevel
        val riskScore: Int
        val signals = mutableListOf<String>()

        when {
            isCashbackTrap -> {
                riskLevel = RiskLevel.CRITICAL
                riskScore = 96
                signals.add("payment_intent_inversion")
                signals.add("deceptive_reward_trigger")
                if (isKnownBlacklist) signals.add("local_threat_blacklist_match")
            }
            isCustomerCareScam -> {
                riskLevel = RiskLevel.CRITICAL
                riskScore = 95
                signals.add("payment_intent_inversion")
                signals.add("fake_support_impersonation")
                if (isKnownBlacklist) signals.add("local_threat_blacklist_match")
            }
            isElectricityScam -> {
                riskLevel = RiskLevel.CRITICAL
                riskScore = 92
                signals.add("urgency_disconnection_threat")
                signals.add("unauthorized_utility_vpa")
                signals.add("payment_intent_inversion")
            }
            isTaskScam -> {
                riskLevel = RiskLevel.HIGH
                riskScore = 88
                signals.add("unrealistic_task_income_lure")
                signals.add("ponzi_deposit_solicitation")
                signals.add("external_channel_redirect")
            }
            isPostalScam -> {
                riskLevel = RiskLevel.HIGH
                riskScore = 86
                signals.add("phishing_parcel_lure")
                signals.add("suspicious_tld_domain")
                signals.add("credential_otp_harvesting")
            }
            isKnownBlacklist -> {
                riskLevel = RiskLevel.CRITICAL
                riskScore = 95
                signals.add("local_threat_blacklist_match")
            }
            hasOtpSolicitation -> {
                riskLevel = RiskLevel.HIGH
                riskScore = 85
                signals.add("credential_otp_harvesting")
            }
            hasSuspiciousLink -> {
                riskLevel = RiskLevel.HIGH
                riskScore = 78
                signals.add("suspicious_tld_domain")
            }
            isMerchantWhitelist -> {
                riskLevel = RiskLevel.SAFE
                riskScore = 5
                signals.add("verified_merchant_whitelist")
            }
            upiPayload != null -> {
                riskLevel = RiskLevel.SAFE
                riskScore = 15
                signals.add("standard_payment_request")
            }
            else -> {
                riskLevel = RiskLevel.SAFE
                riskScore = 10
            }
        }

        val protectionAction = when (riskLevel) {
            RiskLevel.CRITICAL -> ProtectionAction.DISCOURAGE_PROCEED
            RiskLevel.HIGH -> ProtectionAction.REQUIRE_CONFIRMATION
            RiskLevel.WARNING -> ProtectionAction.WARN_CAUTION
            else -> ProtectionAction.ALLOW
        }

        val summary = when {
            isCashbackTrap -> "Critical Payment-Intent Mismatch: Stated reward debits ₹${amount?.toInt() ?: "5,000"}"
            isElectricityScam -> "Urgent Disconnection Trap: Unauthorized personal UPI payment"
            isCustomerCareScam -> "Fake Support Refund Trap: Remote debit collection of ₹${amount?.toInt() ?: "9,999"}"
            isTaskScam -> "Telegram Work-From-Home Task Trap: Advance deposit Ponzi"
            isPostalScam -> "Postal Delivery KYC Phishing: Malicious credential lure"
            riskLevel == RiskLevel.CRITICAL -> "Critical Fraud Risk Detected"
            riskLevel == RiskLevel.HIGH -> "High Risk Phishing Pattern Detected"
            else -> "Verified Safe Transaction"
        }

        val instruction = when (riskLevel) {
            RiskLevel.CRITICAL -> "STOP — Do not enter your UPI PIN or approve this transaction."
            RiskLevel.HIGH -> "CAUTION — Do not click external links or send advance security deposits."
            RiskLevel.WARNING -> "Verify the recipient and transaction details carefully."
            else -> "Verify the recipient name and amount on your banking app before paying."
        }

        val whyItMatters = when {
            isCashbackTrap -> "The sender promised ₹${amount?.toInt() ?: "5,000"} cashback/reward, but this UPI code triggers an outgoing DEBIT of ₹${amount?.toInt() ?: "5,000"} from your account. You NEVER need to enter a UPI PIN to receive money."
            isElectricityScam -> "Scammer uses artificial panic (power disconnection tonight) to rush payment to an unauthorized personal UPI account rather than the official electricity board biller."
            isCustomerCareScam -> "Scammer poses as customer support issuing a refund, but scanning this QR debits ₹${amount?.toInt() ?: "9,999"} from your bank account."
            isTaskScam -> "Promises high daily income for liking videos, but demands upfront ₹${amount?.toInt() ?: "500"} 'activation deposits' that will never be returned."
            isPostalScam -> "Fake delivery notification with a phishing website designed to harvest your bank account details and OTPs."
            else -> "Standard commercial transaction with no detected malicious intent."
        }

        val nodes = mutableListOf<ScamChainNodeDto>()
        val evidence = mutableListOf<EvidenceItemDto>()

        // 1. Initial Ingress Hook
        val hookTitle = when {
            isCashbackTrap -> "Claim ₹${amount?.toInt() ?: "5,000"} Cashback / Reward"
            isElectricityScam -> "Power Disconnection Notice (Tonight 9:30 PM)"
            isTaskScam -> "Earn ₹3,000-₹8,000 Daily From Home"
            isPostalScam -> "India Post Parcel Delivery On Hold"
            isCustomerCareScam -> "Customer Support Failed Txn Refund"
            isMerchantWhitelist -> "Merchant Checkout"
            else -> "Incoming Payment Prompt"
        }
        nodes.add(
            ScamChainNodeDto(
                node_id = "node_msg_1",
                node_type = "MESSAGE",
                state = "OBSERVED",
                confidence = 1.0,
                provenance = "LOCAL_EDGE_CLASSIFIER",
                entity_reference = hookTitle,
                evidence_references = listOf("ev_orig_msg")
            )
        )
        evidence.add(
            EvidenceItemDto(
                evidence_id = "ev_orig_msg",
                evidence_type = "ORIGINAL_CONTENT",
                data = request.contentValue.take(150),
                explanation = "Initial message or payment request inspected",
                source_category = "LOCAL_EDGE_CLASSIFIER"
            )
        )

        // 2. Link node (if URL present)
        val linkRegex = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)
        val extractedLink = linkRegex.find(request.contentValue)?.value
        if (extractedLink != null) {
            nodes.add(
                ScamChainNodeDto(
                    node_id = "node_link_2",
                    node_type = "URL",
                    state = "OBSERVED",
                    confidence = 0.95,
                    provenance = "LOCAL_EDGE_CLASSIFIER",
                    entity_reference = extractedLink,
                    evidence_references = listOf("ev_url")
                )
            )
            evidence.add(
                EvidenceItemDto(
                    evidence_id = "ev_url",
                    evidence_type = "URL_TARGET",
                    data = extractedLink,
                    explanation = "Unverified external destination link",
                    source_category = "LOCAL_EDGE_CLASSIFIER"
                )
            )
        }

        // 3. Target UPI address
        if (payeeVpa != null) {
            nodes.add(
                ScamChainNodeDto(
                    node_id = "node_vpa_3",
                    node_type = "UPI_REQUEST",
                    state = "OBSERVED",
                    confidence = 0.98,
                    provenance = "UPI_INTENT_DECODER",
                    entity_reference = payeeVpa,
                    evidence_references = listOf("ev_vpa")
                )
            )
            evidence.add(
                EvidenceItemDto(
                    evidence_id = "ev_vpa",
                    evidence_type = "UPI_IDENTIFIER",
                    data = payeeVpa,
                    explanation = "Destination UPI Virtual Payment Address",
                    source_category = "UPI_INTENT_DECODER"
                )
            )
        }

        // 4. Payment Action / Outbound Debit
        if (isDebit) {
            val amountFormatted = if (amount != null) "₹${amount.toInt()}" else "₹${amount ?: ""}"
            val actionLabel = if (isMismatch) "Outbound Debit ($amountFormatted)" else "Merchant Payment ($amountFormatted)"
            nodes.add(
                ScamChainNodeDto(
                    node_id = "node_action_4",
                    node_type = "PAYMENT_ACTION",
                    state = "INFERRED",
                    confidence = 0.98,
                    provenance = "UPI_INTENT_DECODER",
                    entity_reference = actionLabel,
                    evidence_references = listOf("ev_mismatch")
                )
            )
            if (isMismatch) {
                evidence.add(
                    EvidenceItemDto(
                        evidence_id = "ev_mismatch",
                        evidence_type = "MISMATCH_DATA",
                        data = "Stated: $statedIntent → Actual: $actionLabel",
                        explanation = "Payment direction inversion verified",
                        source_category = "UPI_INTENT_DECODER"
                    )
                )
            }
        }

        return ScanResult(
            scanId = scanId,
            timestamp = timestamp,
            riskLevel = riskLevel,
            riskScore = riskScore,
            riskConfidence = 0.95,
            signals = signals,
            humanExplanation = whyItMatters,
            recommendedAction = instruction,
            protectionAction = protectionAction,
            detectedSummary = summary,
            whyItMatters = whyItMatters,
            userInstruction = instruction,
            mismatchStatus = if (isMismatch) MismatchStatus.DETECTED else MismatchStatus.NOT_DETECTED,
            statedIntent = statedIntent,
            actualPaymentAction = if (isDebit) "OUTBOUND_DEBIT_COLLECT" else "NONE",
            paymentDirection = if (isDebit) "OUTBOUND_DEBIT" else "NONE",
            mismatchAmount = amount,
            recipientVpa = payeeVpa,
            scamChainNodes = nodes,
            scamChainEdges = emptyList(),
            evidenceItems = evidence,
            isLocalEdgeResult = true
        )
    }
}
