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

    // Determine category based on signals
    let category: IncidentResponseRecommendation['incident_category'] = 'UNKNOWN';
    let urgency: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' = risk.risk_severity === 'UNKNOWN' ? 'LOW' : risk.risk_severity;

    const hasTrading = signals.some(s => s.includes('trading') || s.includes('crypto') || s.includes('broker') || s.includes('investment') || s.includes('sebi'));
    const hasImpersonation = signals.some(s => s.includes('digital_arrest') || s.includes('customs') || s.includes('authority_impersonation') || s.includes('police'));
    const hasKyc = signals.some(s => s.includes('kyc') || s.includes('otp') || s.includes('credential'));
    const hasUpi = signals.some(s => s.includes('upi') || s.includes('payment_intent_mismatch') || s.includes('imposter_emergency') || s.includes('electricity'));

    if (hasImpersonation) {
      category = 'AUTHORITY_IMPERSONATION';
    } else if (hasKyc) {
      category = 'KYC_ACCOUNT_TAKEOVER';
    } else if (hasTrading) {
      category = 'TRADING_FRAUD';
    } else if (hasUpi) {
      category = 'UPI_FRAUD';
    }

    // Populate recommendation fields
    let immediateAction = 'Stop all interaction with the sender.';
    let evidencePreservation = 'Take screenshots of the conversation and any shared links/numbers before blocking the contact.';
    let paymentAction = 'Do not authorize any payments or share banking details.';
    let reportingDest = 'National Cyber Crime Reporting Portal (cybercrime.gov.in) or call 1930.';
    let reportingReason = 'General suspicious activity detected.';

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
