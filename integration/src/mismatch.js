/**
 * Payment Intent Mismatch Analyzer
 * Compares user's perceived / stated interaction against the actual underlying UPI protocol action.
 */

class PaymentIntentMismatchAnalyzer {
  analyze(extractedData, evidenceIds = []) {
    const hasUpi = !!extractedData.upi;
    const hasVpa = extractedData.vpas.length > 0;
    const statedCredit = extractedData.intentSignals.statedCredit;
    const pinPhishing = extractedData.intentSignals.pinPhishing;
    const primaryAmount = extractedData.amounts[0] || (extractedData.upi ? extractedData.upi.am : null);
    const recipientVpa = extractedData.vpas[0] || (extractedData.upi ? extractedData.upi.pa : null);

    // If there is no payment payload or VPA observed
    if (!hasUpi && !hasVpa) {
      return {
        status: 'NOT_OBSERVED',
        payment_direction: 'NONE',
        confidence: 1.0,
        provenance: 'RULE_ENGINE_V1',
        evidence: evidenceIds
      };
    }

    // A UPI URL (upi://pay) or QR scan is ALWAYS an OUTBOUND DEBIT instruction from the payer's perspective.
    const isOutboundAction = hasUpi || (hasVpa && (pinPhishing || extractedData.contentType === 'QR'));

    if (statedCredit && isOutboundAction) {
      // Classic Intent Mismatch Scam: Message says "Receive / Won / Cashback", but action will DEBIT user's account!
      const amountStr = primaryAmount ? 'Rs. ' + primaryAmount : 'funds';
      return {
        status: 'DETECTED',
        stated_intent: 'Receive ' + amountStr + ' reward/cashback/credit into account',
        actual_payment_action: 'Outbound UPI debit request paying ' + amountStr + ' to ' + (recipientVpa || 'third-party VPA'),
        payment_direction: 'OUTBOUND_DEBIT',
        amount: primaryAmount !== null ? Number(primaryAmount) : undefined,
        recipient_vpa: recipientVpa || undefined,
        confidence: 0.95,
        provenance: 'INTENT_SEMANTIC_ANALYSIS_V1',
        evidence: evidenceIds
      };
    }

    if (isOutboundAction) {
      return {
        status: 'NOT_DETECTED',
        stated_intent: 'Make payment or transfer funds',
        actual_payment_action: 'Outbound UPI debit to ' + (recipientVpa || 'merchant/payee'),
        payment_direction: 'OUTBOUND_DEBIT',
        amount: primaryAmount !== null ? Number(primaryAmount) : undefined,
        recipient_vpa: recipientVpa || undefined,
        confidence: 0.90,
        provenance: 'PROTOCOL_INSPECTION_V1',
        evidence: evidenceIds
      };
    }

    return {
      status: 'UNKNOWN',
      payment_direction: 'UNKNOWN',
      confidence: 0.60,
      provenance: 'HEURISTIC_EVALUATOR_V1',
      evidence: evidenceIds
    };
  }
}

module.exports = PaymentIntentMismatchAnalyzer;
