import { ScanResponse } from '../models/types';

export interface IncidentResponseRecommendation {
  incident_category: 'UPI_FRAUD' | 'TRADING_FRAUD' | 'AUTHORITY_IMPERSONATION' | 'KYC_ACCOUNT_TAKEOVER' | 'UNKNOWN' | 'BENIGN';
  immediate_action: string;
  evidence_preservation_guidance: string;
  payment_account_protection_action: string;
  reporting_destination: string;
  reporting_reason: string;
  urgency: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  supporting_evidence_references: string[];
}

export class IncidentResponseService {
  public generateRecommendation(scanResponse: ScanResponse): IncidentResponseRecommendation {
    const risk = scanResponse.risk_assessment;
    const asi = scanResponse.adaptive_scam_intelligence;
    const pim = scanResponse.payment_intent_mismatch;
    const signals = risk.signals || [];
    
    // Default / Benign
    if (risk.risk_severity === 'LOW') {
      return {
        incident_category: 'BENIGN',
        immediate_action: 'No immediate action required.',
        evidence_preservation_guidance: 'None needed.',
        payment_account_protection_action: 'Proceed with normal caution.',
        reporting_destination: 'N/A',
        reporting_reason: 'No fraud detected.',
        urgency: 'LOW',
        supporting_evidence_references: []
      };
    }

    let category: IncidentResponseRecommendation['incident_category'] = 'UNKNOWN';
    const urgency: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' = risk.risk_severity === 'UNKNOWN' ? 'LOW' : risk.risk_severity;

    let immediateAction = 'Stop all interaction with the sender.';
    let evidencePreservation = 'Take screenshots of the conversation and any shared links/numbers before blocking the contact.';
    let paymentAction = 'Do not authorize any payments or share banking details.';
    let reportingDest = 'National Cyber Crime Reporting Portal (cybercrime.gov.in) or call 1930.';
    let reportingReason = 'General suspicious activity detected.';

    if (asi) {
      const arch = asi.archetype;
      if (arch === 'Digital Arrest / Authority Impersonation') {
        category = 'AUTHORITY_IMPERSONATION';
        evidencePreservation = 'Record the caller ID, message headers, and any official-looking documents sent to you.';
        paymentAction = 'Freeze any accounts if you have already shared sensitive financial information.';
        reportingReason = 'Extortion and impersonation of government officials/law enforcement.';
      } else if (arch === 'Credential Harvesting / OTP Scam' || arch === 'Utility Disconnection Scam' || arch === 'Fake Customer Support / Remote Access Scam' || (arch === 'Trading/Investment Fraud' && signals.includes('kyc_phishing'))) {
        category = 'KYC_ACCOUNT_TAKEOVER';
        evidencePreservation = 'Keep a record of the phone number calling you, the SMS requesting action, and note any apps they told you to install.';
        paymentAction = 'If you shared an OTP or installed a remote desktop app, contact your bank IMMEDIATELY to block your accounts and cards. Uninstall the app.';
        reportingReason = 'Attempted account takeover via credential theft or remote access.';
      } else if (arch === 'Trading/Investment Fraud') {
        category = 'TRADING_FRAUD';
        evidencePreservation = 'Document the broker URLs, crypto wallet addresses, and group links provided.';
        paymentAction = 'If funds were deposited, contact your bank immediately to attempt a freeze on the outbound transfer.';
        reportingDest = 'SEBI SCORES portal (scores.gov.in) and National Cyber Crime Reporting Portal.';
        reportingReason = 'Unregistered investment advice and fraudulent trading scheme.';
      } else if (arch === 'QR Code Receive Money Scam' || arch === 'UPI / Payment Fraud' || arch === 'Payment Intent Mismatch (Refund/Prize Scam)') {
        category = 'UPI_FRAUD';
        evidencePreservation = 'Screenshot the UPI collect request, the VPA (UPI ID), the original message, and the QR code if applicable.';
        paymentAction = 'Contact your bank to report the fraudulent VPA. Do not send any test transactions.';
        reportingReason = 'Attempted financial fraud via UPI manipulation.';
      }

      if (asi.recommended_action) {
        immediateAction = asi.recommended_action;
        if (asi.next_likely_step) {
          immediateAction += ' Anticipate: ' + asi.next_likely_step;
        }
      }
    } else {
      // Fallback to legacy keyword matching if ASI is missing
      const hasTrading = signals.some(s => s.includes('trading') || s.includes('crypto') || s.includes('broker') || s.includes('investment') || s.includes('sebi'));
      const hasImpersonation = signals.some(s => s.includes('digital_arrest') || s.includes('customs') || s.includes('authority_impersonation') || s.includes('police'));
      const hasKyc = signals.some(s => s.includes('kyc') || s.includes('otp') || s.includes('credential') || s.includes('customer_support'));
      const hasUpi = signals.some(s => s.includes('upi') || s.includes('payment_intent_mismatch') || s.includes('imposter_emergency') || s.includes('electricity') || s.includes('qr_receive'));

      if (pim && pim.status === 'DETECTED') {
          category = 'UPI_FRAUD';
      } else if (hasImpersonation) {
        category = 'AUTHORITY_IMPERSONATION';
      } else if (hasKyc) {
        category = 'KYC_ACCOUNT_TAKEOVER';
      } else if (hasTrading) {
        category = 'TRADING_FRAUD';
      } else if (hasUpi) {
        category = 'UPI_FRAUD';
      }

      switch (category) {
        case 'UPI_FRAUD':
          immediateAction = 'Cancel any pending UPI collect requests and do not enter your UPI PIN.';
          evidencePreservation = 'Screenshot the UPI collect request, the VPA (UPI ID), and the original message.';
          paymentAction = 'Contact your bank to report the fraudulent VPA. Do not send any test transactions.';
          reportingReason = 'Attempted financial fraud via UPI manipulation.';
          break;
        case 'TRADING_FRAUD':
          immediateAction = 'Do not deposit funds or join recommended trading groups (e.g., Telegram/WhatsApp).';
          evidencePreservation = 'Document the broker URLs, crypto wallet addresses, and group links provided.';
          paymentAction = 'If funds were deposited, contact your bank immediately to attempt a freeze on the outbound transfer.';
          reportingDest = 'SEBI SCORES portal (scores.gov.in) and National Cyber Crime Reporting Portal.';
          reportingReason = 'Unregistered investment advice and fraudulent trading scheme.';
          break;
        case 'AUTHORITY_IMPERSONATION':
          immediateAction = 'Do not transfer money. Legitimate authorities (Police/Customs/CBI) do not ask for money via UPI/crypto to avoid arrest.';
          evidencePreservation = 'Record the caller ID, message headers, and any official-looking documents sent to you.';
          paymentAction = 'Freeze any accounts if you have already shared sensitive financial information.';
          reportingReason = 'Extortion and impersonation of government officials/law enforcement.';
          break;
        case 'KYC_ACCOUNT_TAKEOVER':
          immediateAction = 'Do not share OTPs, PINs, or download any remote desktop apps (e.g., AnyDesk, TeamViewer).';
          evidencePreservation = 'Keep a record of the phone number calling you and the SMS requesting KYC update.';
          paymentAction = 'If you shared an OTP, contact your bank IMMEDIATELY to block your accounts and cards.';
          reportingReason = 'Attempted account takeover via KYC phishing/credential theft.';
          break;
        case 'UNKNOWN':
          reportingReason = 'Suspicious indicators detected requiring further review.';
          break;
      }
    }

    // Override if PIM is extremely clear
    if (pim && pim.status === 'DETECTED' && category !== 'UPI_FRAUD') {
        category = 'UPI_FRAUD';
        reportingReason = 'Payment Intent Mismatch detected. User was tricked into sending money.';
    }

    // Collect evidence references from risk assessment
    const evidenceRefs = risk.evidence_references || [];

    return {
      incident_category: category,
      immediate_action: immediateAction,
      evidence_preservation_guidance: evidencePreservation,
      payment_account_protection_action: paymentAction,
      reporting_destination: reportingDest,
      reporting_reason: reportingReason,
      urgency,
      supporting_evidence_references: evidenceRefs
    };
  }
}

export const incidentResponseService = new IncidentResponseService();
