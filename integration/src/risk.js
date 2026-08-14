/**
 * RefGuard Risk Assessment & Protection Decision Engine
 * Produces explainable risk ratings, protective advisory actions, and plain-language guidance.
 */

class RiskEngine {
  evaluate(extractedData, threatAssessment, mismatch, evidenceIds) {
    let score = threatAssessment.threatScore;
    const signals = [];

    // Add threat signals
    for (const t of threatAssessment.threats) {
      signals.push(t.description);
    }

    // Add Mismatch signals
    if (mismatch.status === 'DETECTED') {
      score = Math.max(score, 90);
      signals.push('CRITICAL: Payment Intent Mismatch detected. Stated intent is to receive funds, but action will perform an OUTBOUND DEBIT of ' + (mismatch.amount ? 'Rs. ' + mismatch.amount : 'money') + '.');
    }

    // Urgency signals
    if (extractedData.intentSignals.urgency) {
      score += 20;
      signals.push('High psychological pressure and artificial urgency detected.');
    }

    // Normalizing Score (0 to 100 integer)
    score = Math.min(100, Math.max(0, Math.round(score)));

    // Risk Severity
    let risk_severity = 'LOW';
    if (score >= 80) risk_severity = 'CRITICAL';
    else if (score >= 60) risk_severity = 'HIGH';
    else if (score >= 30) risk_severity = 'MEDIUM';
    else risk_severity = 'LOW';

    // Confidence
    const confidence = signals.length > 0 ? 0.94 : 0.85;

    // Human Explanation & Recommended Action
    let human_explanation = '';
    let recommended_action = '';
    let action = 'ALLOW';
    let detected_summary = '';
    let why_it_matters = '';
    let user_instruction = '';

    if (risk_severity === 'CRITICAL') {
      action = 'DISCOURAGE_PROCEED';
      detected_summary = 'High-confidence payment fraud and deceptive debit trap detected.';
      why_it_matters = 'You are being prompted to authorize an outbound money transfer under the false belief that you are receiving a prize, reward, or refund.';
      user_instruction = 'DO NOT enter your UPI PIN. UPI PIN is ONLY required for sending money, never for receiving money.';
      human_explanation = 'Critical danger: ' + signals.join(' ');
      recommended_action = 'Cancel the transaction immediately and block the sender. Report this scam to the community registry.';
    } else if (risk_severity === 'HIGH') {
      action = 'REQUIRE_CONFIRMATION';
      detected_summary = 'Suspicious referral scheme and unverified payment request identified.';
      why_it_matters = 'The link or payment request originates from an untrusted or newly registered domain with characteristics of viral scam campaigns.';
      user_instruction = 'Exercise extreme caution. Do not click unknown links or share OTPs / PINs.';
      human_explanation = 'High risk detected: ' + signals.join(' ');
      recommended_action = 'Verify the authenticity of the offer with official customer support before proceeding.';
    } else if (risk_severity === 'MEDIUM') {
      action = 'WARN_CAUTION';
      detected_summary = 'Unverified link or non-standard UPI identifier detected.';
      why_it_matters = 'The destination uses unusual URL patterns or unverified referral codes.';
      user_instruction = 'Verify the recipient name and purpose before approving any interaction.';
      human_explanation = 'Caution advised: ' + signals.join(' ');
      recommended_action = 'Proceed only if you personally know and trust the sender.';
    } else {
      action = 'ALLOW';
      detected_summary = 'No prominent scam or phishing indicators identified.';
      why_it_matters = 'The content does not match known malicious patterns, deceptive VPAs, or payment intent traps.';
      user_instruction = 'Safe to view. Always confirm the recipient VPA and amount before payment authorization.';
      human_explanation = 'Standard legitimate interaction. No suspicious redirection or debit mismatch observed.';
      recommended_action = 'Standard vigilance recommended.';
      if (signals.length === 0) {
        signals.push('Clean domain and standard payment protocol.');
      }
    }

    const riskAssessment = {
      risk_score: score,
      risk_severity,
      confidence,
      signals,
      evidence_references: evidenceIds.all,
      human_explanation,
      recommended_action
    };

    const protectionDecision = {
      action,
      detected_summary,
      why_it_matters,
      user_instruction
    };

    return {
      riskAssessment,
      protectionDecision
    };
  }
}

module.exports = RiskEngine;
