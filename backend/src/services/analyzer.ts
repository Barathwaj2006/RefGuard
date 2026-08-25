import { v4 as uuidv4 } from 'uuid';
import {
  ScanRequest,
  ScanResponse,
  RiskAssessment,
  ProtectionDecision,
  PaymentIntentMismatch,
  ScamChain,
  EvidencePack,
  AdaptiveScamIntelligence
} from '../models/types';
import { communityStore } from './communityStore';
import { extractTradingFraudSignals } from './extractors/tradingFraudExtractor';
import { extractUpiFraudSignals } from './extractors/upiFraudExtractor';
import { sanitizeText } from './extractors/piiSanitizer';
import { shouldEscalateToGemini, analyzeWithGemini, GeminiVerdict } from './geminiReasoningService';
import { EvidenceAggregator } from './evidenceAggregator';

interface ParsedEntities {
  vpa?: string;
  url?: string;
  amount?: number;
  referralCode?: string;
  urgencyWords: string[];
  isCollectRequest: boolean;
  hasOtpSolicitation: boolean;
}

export class AnalyzerService {
  private extractEntities(request: ScanRequest): ParsedEntities {
    const text = request.content_value;
    const entities: ParsedEntities = {
      urgencyWords: [],
      isCollectRequest: false,
      hasOtpSolicitation: false,
    };

    // UPI pay URI parser
    const upiMatch = text.match(/upi:\/\/pay\?([^ \n\r\t]+)/i);
    if (upiMatch) {
      const params = new URLSearchParams(upiMatch[1]);
      entities.vpa = params.get('pa') || undefined;
      const am = params.get('am');
      if (am) entities.amount = parseFloat(am);
    } else {
      // General VPA pattern
      const vpaMatch = text.match(/[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64}/);
      if (vpaMatch) {
        entities.vpa = vpaMatch[0];
      }
    }

    // URL pattern
    const urlMatch = text.match(/https?:\/\/[^\s]+/i);
    if (urlMatch) {
      entities.url = urlMatch[0];
    }

    // Amount extraction
    if (!entities.amount) {
      const amtMatch = text.match(/(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{2})?)/i);
      if (amtMatch) {
        entities.amount = parseFloat(amtMatch[1].replace(/,/g, ''));
      }
    }

    // Urgency & Phishing keywords
    const urgencyKeywords = ['urgent', 'immediately', 'expires', 'suspended', 'blocked', 'lottery', 'prize', 'winner', 'cashback', 'bonus'];
    urgencyKeywords.forEach(kw => {
      if (new RegExp('\\b' + kw + '\\b', 'i').test(text)) {
        entities.urgencyWords.push(kw);
      }
    });

    if (/\b(?:otp|one time password|pin)\b/i.test(text) && /\b(?:share|send|enter|verify)\b/i.test(text)) {
      entities.hasOtpSolicitation = true;
    }

    if (/\b(?:collect\s*(?:request|money)?|request\s+money|pay\s+now|approve\s+debit|authorize\s+debit|upi\s+debit)\b/i.test(text) || upiMatch) {
      entities.isCollectRequest = true;
    }

    return entities;
  }

  /**
   * Analyze content for fraud/scam signals.
   * Now async to support Gemini escalation on ambiguous cases.
   */
  public async analyze(request: ScanRequest): Promise<ScanResponse> {
    const scanId = uuidv4();
    const timestamp = new Date().toISOString();
    const entities = this.extractEntities(request);
    const text = request.content_value.toLowerCase();
    
    const evidenceAggregator = new EvidenceAggregator(scanId, timestamp);
    const sanitizedContent = sanitizeText(request.content_value);
    evidenceAggregator.addEvidence('ORIGINAL_CONTENT', 'ORIGINAL', sanitizedContent.sanitizedText.slice(0, 120));

    // Normalize Source Context
    const sourceRaw = request.source_context || 'unknown';
    let normalizedSource = 'Unknown';
    if (/whatsapp/i.test(sourceRaw)) normalizedSource = 'WhatsApp';
    else if (/telegram/i.test(sourceRaw)) normalizedSource = 'Telegram';
    else if (/mms|sms|message/i.test(sourceRaw)) normalizedSource = 'SMS';
    else if (/mail/i.test(sourceRaw)) normalizedSource = 'Email';
    else if (/browser|web|chrome|safari/i.test(sourceRaw)) normalizedSource = 'Web Browser';
    
    evidenceAggregator.addEvidence('EXTRACTED_ENTITY', 'SOURCE', `Source Context: ${normalizedSource}`);
    
    if (entities.url) evidenceAggregator.addEvidence('URL', 'URL', entities.url);
    if (entities.vpa) evidenceAggregator.addEvidence('UPI_IDENTIFIER', 'UPI', entities.vpa);
    if (entities.amount) evidenceAggregator.addEvidence('EXTRACTED_ENTITY', 'PAYMENT', `Amount: ${entities.amount}`);
    if (entities.urgencyWords.length > 0) evidenceAggregator.addEvidence('EXTRACTED_ENTITY', 'URGENCY', `Urgency Words: ${entities.urgencyWords.join(', ')}`);
    if (entities.hasOtpSolicitation) evidenceAggregator.addEvidence('EXTRACTED_ENTITY', 'CREDENTIAL', 'OTP Solicitation Detected');
    if (entities.isCollectRequest) evidenceAggregator.addEvidence('EXTRACTED_ENTITY', 'PAYMENT', 'Payment/Collect Request');

    // --- Trading Fraud Extraction ---
    const tradingSignals = extractTradingFraudSignals(request.content_value);

    // --- UPI / Telecom Fraud Extraction ---
    const upiSignals = extractUpiFraudSignals(request.content_value);

    // --- Threat Detection ---
    const isCommunityReported = entities.vpa ? communityStore.hasIndicator(entities.vpa) : (entities.url ? communityStore.hasIndicator(entities.url) : false);
    const hasSuspiciousTLD = entities.url ? /\.(tk|xyz|top|work|click|gq|ml|cf)\b/i.test(entities.url) : false;
    const isLegitimateMerchant = entities.vpa ? /(swiggy|zomato|amazon|flipkart|uber|ola)@/i.test(entities.vpa) : false;
    const hasRewardClaims = /\b(won|winner|claim|reward|cashback|lottery|prize|refund)\b/i.test(text);

    // Mismatch Detection
    const isMismatch = hasRewardClaims && entities.isCollectRequest;

    if (isCommunityReported) evidenceAggregator.addEvidence('RISK_SIGNAL', 'COMMUNITY', 'Indicator is reported by the community');
    if (hasSuspiciousTLD) evidenceAggregator.addEvidence('RISK_SIGNAL', 'URL_RISK', 'Suspicious Top-Level Domain');
    if (hasRewardClaims) evidenceAggregator.addEvidence('EXTRACTED_ENTITY', 'REWARD', 'Reward or Prize Claims');
    if (isMismatch) evidenceAggregator.addEvidence('RISK_SIGNAL', 'INTENT_MISMATCH', 'Payment Intent Mismatch (Reward claimed but debit requested)');

    if (tradingSignals.hasTradingFraudSignals) {
      evidenceAggregator.addEvidence('RISK_SIGNAL', 'TRADING', `Trading Fraud Signals: ${tradingSignals.matchedKeywords.join(', ')}`);
      if (tradingSignals.detectedCryptoAddresses.length > 0) evidenceAggregator.addEvidence('EXTRACTED_ENTITY', 'TRADING', `Crypto addresses: ${tradingSignals.detectedCryptoAddresses.join(', ')}`);
      if (tradingSignals.detectedBrokerNames.length > 0) evidenceAggregator.addEvidence('EXTRACTED_ENTITY', 'TRADING', `Broker references: ${tradingSignals.detectedBrokerNames.join(', ')}`);
    }

    if (upiSignals.hasUpiFraudSignals) {
      evidenceAggregator.addEvidence('RISK_SIGNAL', 'SOCIAL_ENG', `Social Engineering Signals: ${upiSignals.matchedKeywords.join(', ')}`);
    }

    let riskScore = 10;
    let riskSeverity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' = 'LOW';
    const signals: string[] = [];

    // --- Scoring: Existing deterministic rules ---
    const isLegitimateDepository = /\b(cdsl|nsdl)\b/i.test(text) && !tradingSignals.depositPaymentRequest && !tradingSignals.cryptoWalletAddress && !tradingSignals.guaranteedReturnClaim && !entities.isCollectRequest && !entities.hasOtpSolicitation;
    if (isCommunityReported) {
      riskScore = 95;
      riskSeverity = 'CRITICAL';
      signals.push('community_blacklist_match');
    } else if (isMismatch) {
      riskScore = 90;
      riskSeverity = 'CRITICAL';
      signals.push('payment_intent_mismatch', 'deceptive_reward_trigger');
    } else if (entities.hasOtpSolicitation) {
      riskScore = 85;
      riskSeverity = 'HIGH';
      signals.push('credential_otp_solicitation');
    } else if (hasSuspiciousTLD) {
      riskScore = 80;
      riskSeverity = 'HIGH';
      signals.push('suspicious_tld_domain');
    } else if (entities.urgencyWords.length > 0) {
      riskScore = 55;
      riskSeverity = 'MEDIUM';
      signals.push('urgency_manipulation');
    } else if (isLegitimateMerchant || isLegitimateDepository) {
      riskScore = 5;
      riskSeverity = 'LOW';
      signals.push(isLegitimateDepository ? 'verified_depository_alert' : 'verified_merchant_whitelist');
    }

    // --- Scoring: Trading fraud signals (additive to existing) ---
    if (tradingSignals.hasTradingFraudSignals && !isLegitimateDepository) {
      signals.push(...tradingSignals.matchedKeywords);

      if (tradingSignals.guaranteedReturnClaim || tradingSignals.fakeIpoAllotment) {
        if (riskScore < 85) {
          riskScore = Math.max(riskScore, 85);
          riskSeverity = 'HIGH';
        }
      }

      if (tradingSignals.cryptoWalletAddress && tradingSignals.depositPaymentRequest) {
        riskScore = Math.max(riskScore, 90);
        riskSeverity = 'CRITICAL';
      }

      if (tradingSignals.kycRequest && (tradingSignals.sebiReference || tradingSignals.fakeBrokerReference)) {
        riskScore = Math.max(riskScore, 88);
        riskSeverity = 'HIGH';
      }

      if (tradingSignals.tradingTipGroup) {
        riskScore = Math.max(riskScore, 70);
        if (riskSeverity === 'LOW') riskSeverity = 'MEDIUM';
      }

      if (tradingSignals.dematAccountReference && tradingSignals.depositPaymentRequest) {
        riskScore = Math.max(riskScore, 80);
        riskSeverity = 'HIGH';
      }

      if (tradingSignals.signalCount >= 3 && riskScore < 85) {
        riskScore = Math.max(riskScore, 85);
        riskSeverity = 'HIGH';
      }

      if (tradingSignals.signalCount === 1 && riskScore < 55) {
        riskScore = Math.max(riskScore, 55);
        if (riskSeverity === 'LOW') riskSeverity = 'MEDIUM';
      }
    }

    // --- Scoring: UPI/Telecom fraud signals (additive) ---
    if (upiSignals.hasUpiFraudSignals) {
      signals.push(...upiSignals.matchedKeywords);

      if (upiSignals.digitalArrestScam || upiSignals.customsParcelScam) {
        riskScore = Math.max(riskScore, 95);
        riskSeverity = 'CRITICAL';
      }

      if (upiSignals.electricityBillScam || upiSignals.telecomKycScam || upiSignals.refundCashbackScam) {
        if (entities.vpa || entities.url || entities.isCollectRequest) {
          riskScore = Math.max(riskScore, 90);
          riskSeverity = 'CRITICAL';
        } else {
          riskScore = Math.max(riskScore, 80);
          riskSeverity = 'HIGH';
        }
      }

      if (upiSignals.familyEmergencyScam) {
        riskScore = Math.max(riskScore, 75);
        if (riskSeverity !== 'CRITICAL') riskSeverity = 'HIGH';
      }
    }

    // --- Source-Aware Weighting ---
    if (normalizedSource === 'Telegram' && tradingSignals.hasTradingFraudSignals) {
      riskScore = Math.max(riskScore, 85);
      if (riskSeverity !== 'CRITICAL') riskSeverity = 'HIGH';
      signals.push('telegram_trading_scam');
    }

    if (normalizedSource === 'WhatsApp' && upiSignals.familyEmergencyScam) {
      riskScore = Math.max(riskScore, 90);
      riskSeverity = 'CRITICAL';
      signals.push('whatsapp_imposter_emergency');
    }

    if (normalizedSource === 'SMS' && (upiSignals.digitalArrestScam || upiSignals.customsParcelScam || upiSignals.electricityBillScam)) {
      riskScore = Math.max(riskScore, 90);
      riskSeverity = 'CRITICAL';
      signals.push('sms_authority_impersonation');
    }

    // --- Gemini Reasoning Escalation ---
    let geminiVerdict: GeminiVerdict | null = null;
    if (shouldEscalateToGemini(riskScore)) {
      const sanitizationResult = sanitizeText(request.content_value);
      geminiVerdict = await analyzeWithGemini({
        sanitizedContent: sanitizationResult.sanitizedText,
        deterministicScore: riskScore,
        existingSignals: signals,
        contentType: request.content_type,
      });

      if (geminiVerdict.gemini_used) {
        riskScore = Math.max(0, Math.min(100, riskScore + geminiVerdict.risk_adjustment));
        signals.push(...geminiVerdict.detected_patterns.map(p => 'gemini_' + p.toLowerCase().replace(/\s+/g, '_')));
        signals.push('gemini_reasoning_applied');
        evidenceAggregator.addEvidence('RISK_SIGNAL', 'GEMINI', `Gemini Reasoning: ${geminiVerdict.reasoning}`);
      }
    }

    // --- Re-classify severity after all adjustments ---
    if (riskScore >= 85) riskSeverity = 'CRITICAL';
    else if (riskScore >= 65) riskSeverity = 'HIGH';
    else if (riskScore >= 40) riskSeverity = 'MEDIUM';
    else riskSeverity = 'LOW';

    // --- Confidence Calculation ---
    const baseConfidence = 0.6;
    const signalBoost = Math.min(0.35, signals.length * 0.05);
    const geminiBoost = geminiVerdict?.gemini_used ? geminiVerdict.confidence * 0.1 : 0;
    const confidence = Math.min(1.0, +(baseConfidence + signalBoost + geminiBoost).toFixed(2));

    let decision: ProtectionDecision;
    const amountStr = entities.amount ? ` ₹${entities.amount}` : ' money';
    if (riskSeverity === 'CRITICAL') {
      const isTradingScam = tradingSignals.hasTradingFraudSignals && tradingSignals.signalCount >= 2;
      const isUpiScam = upiSignals.hasUpiFraudSignals;
      decision = {
        action: 'DISCOURAGE_PROCEED',
        detected_summary: isMismatch
          ? 'Critical Payment-Intent Mismatch Detected'
          : isTradingScam
            ? 'Trading/Investment Fraud Pattern Detected'
            : isUpiScam
              ? 'Social Engineering / Impersonation Fraud Detected'
              : 'Known Scam Signature Identified',
        why_it_matters: isMismatch
          ? `You were told you are receiving money/prize, but this UPI request will DEBIT${amountStr} from your account.`
          : isTradingScam
            ? 'This message contains multiple investment fraud signals including fake returns, unauthorized broker references, or fraudulent platform links.'
            : isUpiScam
              ? 'This matches a known highly-prevalent scam template (e.g. digital arrest, fake electricity bill, or customs seizure) designed to steal your money.'
              : 'This identifier matches confirmed fraud signatures reported by the community.',
        user_instruction: isTradingScam
          ? 'DO NOT deposit money, share KYC documents, or join any trading group promoted here. Report this to SEBI/cybercrime.'
          : isUpiScam
            ? 'DO NOT pay. Legitimate authorities/companies do not demand immediate UPI/bank transfers via WhatsApp/SMS. Report to 1930.'
            : 'DO NOT enter your UPI PIN. Cancel this transaction immediately.'
      };
    } else if (riskSeverity === 'HIGH') {
      const isTradingRelated = tradingSignals.hasTradingFraudSignals;
      decision = {
        action: 'REQUIRE_CONFIRMATION',
        detected_summary: isTradingRelated
          ? 'Suspicious Trading/Investment Pattern Detected'
          : 'High Risk Phishing Pattern Detected',
        why_it_matters: isTradingRelated
          ? 'This content contains indicators of potential investment fraud such as guaranteed returns, unregistered advisors, or suspicious trading platforms.'
          : 'Suspicious elements were found that resemble credential-harvesting or scam links.',
        user_instruction: isTradingRelated
          ? 'Verify any investment advice through SEBI-registered channels only. Never deposit money to unverified platforms.'
          : 'Do not share OTPs, click unknown links, or send funds.'
      };
    } else if (riskSeverity === 'MEDIUM') {
      decision = {
        action: 'WARN_CAUTION',
        detected_summary: 'Suspicious Indicators Found',
        why_it_matters: geminiVerdict?.gemini_used
          ? `AI analysis: ${geminiVerdict.reasoning.slice(0, 200)}`
          : 'The message contains psychological urgency or unverified claims.',
        user_instruction: 'Verify the identity of the sender through official channels before proceeding.'
      };
    } else {
      const isDepository = signals.includes('verified_depository_alert');
      decision = {
        action: 'ALLOW',
        detected_summary: isDepository ? 'Legitimate Depository Notification' : 'No Threat Detected (Safe)',
        why_it_matters: isDepository
          ? 'This message matches standard legitimate transactional alerts from CDSL/NSDL depositories with no fraudulent debit or credential solicitation.'
          : 'Content matches standard legitimate interaction patterns with zero malicious indicators.',
        user_instruction: isDepository
          ? 'No action required. This is an official informational alert from your depository participant.'
          : 'Proceed with normal caution.'
      };
    }

    const riskAssessment: RiskAssessment = {
      risk_score: riskScore,
      risk_severity: riskSeverity,
      confidence,
      signals,
      evidence_references: evidenceAggregator.getAllEvidenceIds(),
      human_explanation: decision.why_it_matters,
      recommended_action: decision.user_instruction
    };

    // Mismatch Object
    const paymentIntentMismatch: PaymentIntentMismatch = {
      status: isMismatch ? 'DETECTED' : 'NOT_DETECTED',
      stated_intent: hasRewardClaims ? 'RECEIVE_FUNDS_OR_PRIZE' : 'STANDARD_PAYMENT',
      actual_payment_action: entities.isCollectRequest ? 'OUTBOUND_DEBIT_COLLECT' : 'NONE',
      payment_direction: entities.isCollectRequest ? 'OUTBOUND_DEBIT' : 'NONE',
      amount: entities.amount,
      recipient_vpa: entities.vpa,
      confidence: isMismatch ? 0.95 : 0.8,
      provenance: 'rule_engine',
      evidence: evidenceAggregator.getEvidenceIdsByCategories(['PAYMENT', 'REWARD', 'INTENT_MISMATCH'])
    };

    // Scam Chain DAG
    const scamChain: ScamChain = {
      nodes: [
        { node_id: 'node_msg', node_type: 'MESSAGE', entity_reference: `${normalizedSource} Message`, evidence_references: evidenceAggregator.getEvidenceIdsByCategories(['ORIGINAL', 'SOURCE']) },
        ...(entities.url ? [{ node_id: 'node_url', node_type: 'SHORT_LINK' as const, entity_reference: entities.url, evidence_references: evidenceAggregator.getEvidenceIdsByCategory('URL') }] : []),
        ...(entities.vpa ? [{ node_id: 'node_upi', node_type: 'UPI_REQUEST' as const, entity_reference: entities.vpa, evidence_references: evidenceAggregator.getEvidenceIdsByCategory('UPI') }] : []),
        ...(entities.isCollectRequest ? [{ node_id: 'node_pay', node_type: 'PAYMENT_ACTION' as const, entity_reference: 'UPI Debit', evidence_references: evidenceAggregator.getEvidenceIdsByCategory('PAYMENT') }] : []),
        ...(tradingSignals.hasTradingFraudSignals ? [{ node_id: 'node_trading', node_type: 'REFERRAL' as const, entity_reference: 'Trading Fraud Signal', evidence_references: evidenceAggregator.getEvidenceIdsByCategory('TRADING') }] : []),
        ...(upiSignals.hasUpiFraudSignals ? [{ node_id: 'node_social_eng', node_type: 'MESSAGE' as const, entity_reference: 'Social Engineering Pattern', evidence_references: evidenceAggregator.getEvidenceIdsByCategory('SOCIAL_ENG') }] : [])
      ],
      edges: [
        ...(entities.url ? [{ from_node: 'node_msg', to_node: 'node_url', relationship: 'CONTAINS_LINK', confidence: 0.95, provenance: 'extraction', evidence_references: evidenceAggregator.getEvidenceIdsByCategory('URL') }] : []),
        ...(entities.vpa ? [{ from_node: entities.url ? 'node_url' : 'node_msg', to_node: 'node_upi', relationship: 'INITIATES_UPI', confidence: 0.9, provenance: 'extraction', evidence_references: evidenceAggregator.getEvidenceIdsByCategory('UPI') }] : []),
        ...(entities.isCollectRequest ? [{ from_node: 'node_upi', to_node: 'node_pay', relationship: 'TRIGGERS_DEBIT', confidence: 0.95, provenance: 'intent_analysis', evidence_references: evidenceAggregator.getEvidenceIdsByCategory('PAYMENT') }] : []),
        ...(tradingSignals.hasTradingFraudSignals ? [{ from_node: 'node_msg', to_node: 'node_trading', relationship: 'PROMOTES_TRADING_FRAUD', confidence: 0.85, provenance: 'trading_fraud_extractor', evidence_references: evidenceAggregator.getEvidenceIdsByCategory('TRADING') }] : []),
        ...(upiSignals.hasUpiFraudSignals ? [{ from_node: 'node_msg', to_node: 'node_social_eng', relationship: 'EMPLOYS_SOCIAL_ENGINEERING', confidence: 0.90, provenance: 'upi_fraud_extractor', evidence_references: evidenceAggregator.getEvidenceIdsByCategory('SOCIAL_ENG') }] : [])
      ]
    };

    // Evidence Pack — PII-safe
    const evidencePack: EvidencePack = evidenceAggregator.hasEvidence() 
      ? evidenceAggregator.buildEvidencePack() 
      : { incident_id: 'inc_' + scanId.slice(0, 8), timestamp, items: [] };

    // Adaptive Scam-Chain Intelligence
    let adaptiveScamIntelligence: AdaptiveScamIntelligence | undefined = undefined;
    if (riskScore >= 40) {
      let archetype = 'General Scams';
      let current_stage = 'Initial Contact';
      let stage_title = 'Initial Contact';
      let stage_index = 1;
      let total_stages = 4;
      let stages_sequence = ['Initial Contact', 'Build Trust', 'Extract Funds', 'Disappear'];
      let previous_likely_stage: string | null = null;
      let next_likely_stage: string | null = 'Build Trust';
      let next_likely_step: string | null = 'The attacker may try to establish a rapport or offer a fake opportunity.';
      let attacker_objective = 'Financial Gain';
      let user_risk = 'Low if ignored. High if engaged.';
      let recommended_action = 'Ignore and block the sender.';
      let confidence_intel = 0.5;
      let reporting_path = 'https://cybercrime.gov.in/';

      if (tradingSignals.hasTradingFraudSignals) {
        archetype = 'Trading/Investment Fraud';
        stages_sequence = ['Initial Contact', 'Fake Platform Onboarding', 'Small Payout (Bait)', 'Large Deposit Request', 'Account Freeze'];
        total_stages = 5;
        
        if (tradingSignals.depositPaymentRequest && tradingSignals.cryptoWalletAddress) {
           current_stage = 'Large Deposit Request';
           stage_title = 'Deposit Request';
           stage_index = 4;
           previous_likely_stage = 'Small Payout (Bait)';
           next_likely_stage = 'Account Freeze';
           next_likely_step = 'They will claim you need to pay taxes or fees to withdraw your funds, but you will never get them back.';
           confidence_intel = 0.9;
        } else if (tradingSignals.tradingTipGroup || tradingSignals.fakeBrokerReference) {
           current_stage = 'Initial Contact / Fake Platform Onboarding';
           stage_title = 'Onboarding';
           stage_index = 2;
           previous_likely_stage = 'Initial Contact';
           next_likely_stage = 'Small Payout (Bait)';
           next_likely_step = 'They will ask you to create an account on their platform and make a small deposit, promising high returns.';
           confidence_intel = 0.8;
        }
        attacker_objective = 'Steal large sums of money through fake investment platforms.';
        user_risk = 'Extremely High. Victims often lose their life savings.';
        recommended_action = 'Do not invest. Report to SEBI and Cybercrime.';
      } else if (upiSignals.hasUpiFraudSignals) {
        if (upiSignals.digitalArrestScam) {
           archetype = 'Digital Arrest / Authority Impersonation';
           stages_sequence = ['Robocall/Message', 'Fake Official Interrogation', 'Isolation & Intimidation', 'Coerced Payment', 'Ongoing Extortion'];
           total_stages = 5;
           current_stage = 'Isolation & Intimidation';
           stage_title = 'Intimidation';
           stage_index = 3;
           previous_likely_stage = 'Fake Official Interrogation';
           next_likely_stage = 'Coerced Payment';
           next_likely_step = 'They will demand a "security deposit" or "fine" to avoid arrest, usually via UPI or bank transfer.';
           attacker_objective = 'Coerce victim into transferring funds under threat of arrest.';
           user_risk = 'Critical. High psychological pressure leading to rapid financial loss.';
           recommended_action = 'Hang up immediately. Police will never arrest you over a phone call or Skype/WhatsApp video call.';
           confidence_intel = 0.95;
        } else if (upiSignals.electricityBillScam) {
           archetype = 'Utility Disconnection Scam';
           stages_sequence = ['Fake SMS', 'Call to "Helpdesk"', 'Remote Access App Install', 'Bank Credential Theft', 'Unauthorized Transfer'];
           total_stages = 5;
           current_stage = 'Fake SMS';
           stage_title = 'Fake SMS';
           stage_index = 1;
           previous_likely_stage = null;
           next_likely_stage = 'Call to "Helpdesk"';
           next_likely_step = 'If you call the number, they will ask you to install a screen-sharing app to "update your bill".';
           attacker_objective = 'Gain remote access to your phone and steal banking credentials.';
           user_risk = 'High. Potential for complete bank account drain.';
           recommended_action = 'Do not call the number. Check your electricity bill on the official website or app.';
           confidence_intel = 0.9;
        } else {
           archetype = 'UPI / Payment Fraud';
           stages_sequence = ['Initial Message', 'Urgency/Fear Tactic', 'Payment Request', 'Funds Stolen'];
           total_stages = 4;
           current_stage = 'Urgency/Fear Tactic';
           stage_title = 'Urgency Tactic';
           stage_index = 2;
           previous_likely_stage = 'Initial Message';
           next_likely_stage = 'Payment Request';
           next_likely_step = 'They will send a UPI collect request or ask for your UPI PIN.';
           attacker_objective = 'Steal funds via UPI transaction.';
           user_risk = 'High.';
           recommended_action = 'Do not enter your UPI PIN. Do not approve unknown collect requests.';
           confidence_intel = 0.8;
        }
      } else if (isMismatch) {
        archetype = 'Payment Intent Mismatch (Refund/Prize Scam)';
        stages_sequence = ['Notification of Prize/Refund', 'Link Clicks/App Open', 'UPI PIN Entry', 'Funds Deducted'];
        total_stages = 4;
        current_stage = 'UPI PIN Entry';
        stage_title = 'PIN Entry';
        stage_index = 3;
        previous_likely_stage = 'Link Clicks/App Open';
        next_likely_stage = 'Funds Deducted';
        next_likely_step = 'Entering your PIN will authorize a deduction from your account, not a credit.';
        attacker_objective = 'Trick user into authorizing a debit while believing it is a credit.';
        user_risk = 'Critical. Immediate financial loss if PIN is entered.';
        recommended_action = 'CANCEL the transaction. You NEVER need a UPI PIN to receive money.';
        confidence_intel = 0.95;
      } else if (entities.hasOtpSolicitation) {
        archetype = 'Credential Harvesting / OTP Scam';
        stages_sequence = ['Initial Contact', 'Fabricate Urgency', 'Request OTP', 'Account Takeover'];
        total_stages = 4;
        current_stage = 'Request OTP';
        stage_title = 'Request OTP';
        stage_index = 3;
        previous_likely_stage = 'Fabricate Urgency';
        next_likely_stage = 'Account Takeover';
        next_likely_step = 'Once you share the OTP, they will log into your account and change passwords or transfer funds.';
        attacker_objective = 'Steal access to user account (bank, email, social media).';
        user_risk = 'High. Risk of immediate account compromise.';
        recommended_action = 'NEVER share an OTP with anyone. No legitimate organization will ask for your OTP.';
        confidence_intel = 0.9;
      }

      adaptiveScamIntelligence = {
        archetype,
        current_stage,
        stage_title,
        stage_index,
        total_stages,
        stages_sequence,
        evidence_detected: evidenceAggregator.getAllEvidenceIds(),
        previous_likely_stage,
        next_likely_stage,
        next_likely_step,
        attacker_objective,
        user_risk,
        recommended_action,
        confidence: confidence_intel,
        reporting_path,
        evidence_backed_status: true,
        provenance: geminiVerdict?.gemini_used ? 'gemini_and_deterministic' : 'deterministic_engine'
      };
    }

    return {
      scan_id: scanId,
      timestamp,
      risk_assessment: riskAssessment,
      protection_decision: decision,
      payment_intent_mismatch: paymentIntentMismatch,
      scam_chain: scamChain,
      evidence_pack: evidencePack,
      adaptive_scam_intelligence: adaptiveScamIntelligence
    };
  }
}
