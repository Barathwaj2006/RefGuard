/**
 * UPI & Telecom Fraud Extractor — Deterministic signal detection for Indian context scams.
 *
 * Extends RefGuard's extraction pipeline with pervasive Indian scam themes:
 * Electricity bill disconnection, Customs/FedEx parcel seizures, Digital Arrest/CBI,
 * Telecom KYC/SIM block, and common Hinglish urgency markers.
 */

export interface UpiFraudSignals {
  hasUpiFraudSignals: boolean;

  // Specific Categories
  electricityBillScam: boolean;
  customsParcelScam: boolean;
  digitalArrestScam: boolean;
  telecomKycScam: boolean;
  refundCashbackScam: boolean; // Hinglish focus
  familyEmergencyScam: boolean; // e.g. "accident, send money to hospital"

  matchedKeywords: string[];
  signalCount: number;
}

// Electricity Bill (often Hindi/Hinglish/English mix)
const ELECTRICITY_PATTERNS = [
  /\b(?:electricity|bijli)\s*(?:power|bill)?\s*(?:will\s*be\s*disconnected|cut|kat\s*jayegi|pending|due)\b/i,
  /\b(?:update|pay|clear)\s*(?:previous\s*month|last\s*month)\s*bill\b/i,
  /\b(?:call|contact)\s*(?:electricity|bijli)\s*(?:officer|board|department|office)\b/i
];

// Customs / Courier (FedEx, BlueDart, India Post)
const CUSTOMS_COURIER_PATTERNS = [
  /\b(?:fedex|bluedart|dhl|india\s*post|courier|parcel|package)\s*(?:seized|stopped|blocked|held|customs?|narcotics?|illegal)\b/i,
  /\b(?:customs?|narcotics?|cbi|police)\s*(?:found|seized|investigating)\s*(?:drugs?|passport|illegal\s*items?)\s*(?:in\s*your\s*)?(?:parcel|package|courier)\b/i,
  /\b(?:pay|clear)\s*(?:customs?|clearance)\s*(?:duty|fee|charge|tax)\s*(?:immediately|urgent|today)\b/i
];

// Digital Arrest / Law Enforcement
const DIGITAL_ARREST_PATTERNS = [
  /\b(?:digital\s*arrest|arrest\s*warrant|fir|supreme\s*court|high\s*court)\b/i,
  /\b(?:cbi|police|cyber\s*crime|narcotics?|rbi|trai)\s*(?:officer|department|headquarters?)\b/i,
  /\b(?:your|aadhar|aadhaar|pan)\s*(?:card|number)?\s*(?:is\s*linked\s*to|involved\s*in|used\s*for)\s*(?:money\s*laundering|illegal\s*activities|scam)\b/i,
  /\b(?:skype|zoom|whatsapp)\s*(?:video\s*call)\s*(?:for\s*investigation|statement)\b/i
];

// Telecom / KYC (SIM Block, 5G upgrade, etc.)
const TELECOM_KYC_PATTERNS = [
  /\b(?:jio|airtel|vi|bsnl|vodafone)\s*(?:sim|number)\s*(?:will\s*be\s*)?(?:blocked|deactivated|suspended)\b/i,
  /\b(?:update|complete)\s*(?:kyc|e-kyc|document)\s*(?:within|in|before)\s*\d+\s*(?:hours|hrs|mins|minutes)\b/i,
  /\b(?:upgrade\s*to\s*5g|claim\s*free\s*data)\s*(?:click|link|download)\b/i,
  /\b(?:forward\s*this|dial|call)\s*(?:\*\d+#?|\*401\*[0-9]+)\b/i // Call forwarding scam
];

// Refund / Cashback / Lottery (Hinglish focus)
const REFUND_CASHBACK_PATTERNS = [
  /\b(?:cashback|refund|lottery|inaam|prize)\s*(?:mila\s*hai|claim\s*karein?|aaya\s*hai|receive\s*hua)\b/i,
  /\b(?:scratch\s*card|lucky\s*draw|kbc|jio\s*lottery)\b/i,
  /\b(?:paise\s*bhejo|scan\s*karein?|pin\s*dalein?)\s*(?:to\s*receive|paane\s*ke\s*liye)\b/i
];

// Emergency Scam
const EMERGENCY_PATTERNS = [
  /\b(?:accident|hospital|emergency|police\s*station|admit)\s*(?:send\s*money|transfer\s*fast|urgent\s*help)\b/i,
  /\b(?:dost|bhai|friend)\s*(?:accident|hospital)\s*(?:paise|money|gpay|phonepe)\b/i
];

export function extractUpiFraudSignals(text: string): UpiFraudSignals {
  const signals: UpiFraudSignals = {
    hasUpiFraudSignals: false,
    electricityBillScam: false,
    customsParcelScam: false,
    digitalArrestScam: false,
    telecomKycScam: false,
    refundCashbackScam: false,
    familyEmergencyScam: false,
    matchedKeywords: [],
    signalCount: 0
  };

  // Electricity
  for (const pattern of ELECTRICITY_PATTERNS) {
    if (pattern.test(text)) {
      signals.electricityBillScam = true;
      signals.matchedKeywords.push('electricity_bill_scam');
      break;
    }
  }

  // Customs / Courier
  for (const pattern of CUSTOMS_COURIER_PATTERNS) {
    if (pattern.test(text)) {
      signals.customsParcelScam = true;
      signals.matchedKeywords.push('customs_courier_scam');
      break;
    }
  }

  // Digital Arrest
  for (const pattern of DIGITAL_ARREST_PATTERNS) {
    if (pattern.test(text)) {
      signals.digitalArrestScam = true;
      signals.matchedKeywords.push('digital_arrest_scam');
      break;
    }
  }

  // Telecom / KYC
  for (const pattern of TELECOM_KYC_PATTERNS) {
    if (pattern.test(text)) {
      signals.telecomKycScam = true;
      signals.matchedKeywords.push('telecom_kyc_scam');
      break;
    }
  }

  // Refund / Cashback
  for (const pattern of REFUND_CASHBACK_PATTERNS) {
    if (pattern.test(text)) {
      signals.refundCashbackScam = true;
      signals.matchedKeywords.push('hinglish_cashback_scam');
      break;
    }
  }

  // Emergency
  for (const pattern of EMERGENCY_PATTERNS) {
    if (pattern.test(text)) {
      signals.familyEmergencyScam = true;
      signals.matchedKeywords.push('emergency_imposter_scam');
      break;
    }
  }

  // Calculate signal count
  const booleanSignals = [
    signals.electricityBillScam,
    signals.customsParcelScam,
    signals.digitalArrestScam,
    signals.telecomKycScam,
    signals.refundCashbackScam,
    signals.familyEmergencyScam
  ];

  signals.signalCount = booleanSignals.filter(Boolean).length;
  signals.hasUpiFraudSignals = signals.signalCount > 0;

  return signals;
}
