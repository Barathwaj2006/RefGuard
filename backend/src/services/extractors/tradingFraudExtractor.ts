/**
 * Trading-Fraud Extractor — Deterministic signal detection for trading/investment scams.
 *
 * Extends RefGuard's extraction pipeline with India-specific trading-fraud patterns:
 * SEBI references, fake brokers, crypto wallets, guaranteed returns, IPO scams,
 * trading-tip groups, demat solicitation, and KYC phishing.
 */

export interface TradingFraudSignals {
  /** Whether any trading-fraud signal was detected */
  hasTradingFraudSignals: boolean;

  /** Individual detected signal categories */
  sebiReference: boolean;
  fakeBrokerReference: boolean;
  brokerAppLink: boolean;
  dematAccountReference: boolean;
  cryptoWalletAddress: boolean;
  guaranteedReturnClaim: boolean;
  fakeIpoAllotment: boolean;
  tradingTipGroup: boolean;
  depositPaymentRequest: boolean;
  kycRequest: boolean;

  /** Extracted entities for evidence pack */
  detectedBrokerNames: string[];
  detectedCryptoAddresses: string[];
  detectedAmountClaims: string[];
  matchedKeywords: string[];

  /** Composite signal count for confidence weighting */
  signalCount: number;
}

// Legitimate broker names used in fake-broker scam contexts
const KNOWN_BROKER_NAMES = [
  'angel broking', 'angel one', 'zerodha', 'upstox', 'groww', '5paisa',
  'motilal oswal', 'sharekhan', 'icici direct', 'hdfc securities',
  'kotak securities', 'axis direct', 'sbi securities', 'iifl securities',
  'paytm money', 'kite', 'coin by zerodha'
];

// Suspicious trading platform keywords (used in fake platform scams)
const FAKE_PLATFORM_KEYWORDS = [
  'trading platform', 'investment platform', 'trading app', 'investment app',
  'forex trading', 'binary options', 'options trading platform',
  'crypto exchange', 'bitcoin exchange'
];

// SEBI-related patterns
const SEBI_PATTERNS = [
  /\bSEBI\s*(?:registered?|certified?|approved?|license|registration\s*(?:no|number|#)?)\b/i,
  /\bSEBI\s*reg(?:istration)?\s*(?:no|number|#)?\s*[:.]?\s*[A-Z0-9-]+/i,
  /\bINZ\d{9,}/i, // SEBI registration number format
];

// Crypto wallet address patterns
const CRYPTO_ADDRESS_PATTERNS = [
  /\b(1|3|bc1)[a-zA-HJ-NP-Z0-9]{25,39}\b/, // Bitcoin
  /\b0x[a-fA-F0-9]{40}\b/, // Ethereum
  /\bT[a-zA-Z0-9]{33}\b/, // Tron/USDT TRC-20
  /\b[LM][a-km-zA-HJ-NP-Z1-9]{26,33}\b/, // Litecoin
];

// Guaranteed return claim patterns
const GUARANTEED_RETURN_PATTERNS = [
  /\bguaranteed?\s+(?:\d+%?\s+)?(?:returns?|profit|income|earnings?)\b/i,
  /\b(?:fixed|assured|certain|risk[\s-]*free)\s+(?:returns?|profit|income|monthly\s+income)\b/i,
  /\b(?:earn|make|get)\s+(?:₹|rs\.?|inr)?\s*\d[\d,]*\s*(?:daily|weekly|monthly|per\s*(?:day|week|month))\b/i,
  /\b\d+%\s*(?:daily|weekly|monthly|per\s*(?:day|week|month))\s*(?:returns?|profit|income|guaranteed?)\b/i,
  /\b(?:double|triple|2x|3x|5x|10x)\s+(?:your\s+)?(?:money|investment|capital)\b/i,
  /\bno\s*(?:risk|loss)\s*(?:trading|investment|guaranteed?)\b/i,
];

// IPO-related scam patterns
const IPO_SCAM_PATTERNS = [
  /\b(?:IPO|initial\s+public\s+offering)\s*(?:allotment|allocation|listing|grey\s*market|pre[\s-]*listing)\b/i,
  /\bpre[\s-]*IPO\s*(?:shares?|stocks?|investment)\b/i,
  /\bIPO\s*(?:guaranteed?|assured?|confirmed?)\s*(?:allotment|allocation)\b/i,
  /\bgrey\s*market\s*premium\s*(?:\d+%?)/i,
  /\bunlisted\s+shares?\b/i,
];

// Demat/account solicitation patterns
const DEMAT_PATTERNS = [
  /\b(?:demat|d[\s-]*mat)\s*(?:account|a\/c)\b/i,
  /\bCDSL|NSDL\b/i,
  /\b(?:depository\s+participant|DP\s+ID)\b/i,
];

// Trading tip group patterns
const TIP_GROUP_PATTERNS = [
  /\b(?:join|subscribe)\b.*?\b(?:group|channel|telegram|whatsapp|link)\b/i,
  /\b(?:free|premium|vip|exclusive)\s+(?:trading|stock|share|market)\s*(?:tips?|signals?|calls?|advice)\b/i,
  /\b(?:trading|stock|options)\s+(?:tips?|signals?|group|telegram)\b/i,
  /\b(?:intraday|swing|positional)\s+(?:tips?|calls?|signals?)\b/i,
  /\bjackpot\s+(?:call|tip|stock)\b/i,
];

// KYC phishing in trading context
const KYC_PHISHING_PATTERNS = [
  /\b(?:complete|update|verify|submit)\s+(?:your\s+)?(?:KYC|e[\s-]*KYC|know\s+your\s+customer)\b/i,
  /\b(?:share|send|submit|upload)\s+(?:your\s+)?(?:aadhaar|aadhar|PAN|pan\s+card|passport|voter\s+id)\b/i,
  /\bKYC\s*(?:pending|required|mandatory|expired|failed|verification)\b/i,
];

// Deposit/payment request patterns in trading context
const DEPOSIT_REQUEST_PATTERNS = [
  /\b(?:deposit|transfer|send|pay)\s+(?:₹|rs\.?|inr)?\s*[\d,]+\s*(?:to\s+(?:start|begin|activate)|(?:for|as)\s+(?:registration|activation|membership|subscription))\b/i,
  /\b(?:minimum|initial)\s+(?:deposit|investment|amount)\s*(?:of)?\s*(?:₹|rs\.?|inr)?\s*[\d,]+/i,
  /\b(?:registration|activation|membership)\s*(?:fee|charge|amount)\s*(?:of|:)?\s*(?:₹|rs\.?|inr)?\s*[\d,]+/i,
];

export function extractTradingFraudSignals(text: string): TradingFraudSignals {
  const signals: TradingFraudSignals = {
    hasTradingFraudSignals: false,
    sebiReference: false,
    fakeBrokerReference: false,
    brokerAppLink: false,
    dematAccountReference: false,
    cryptoWalletAddress: false,
    guaranteedReturnClaim: false,
    fakeIpoAllotment: false,
    tradingTipGroup: false,
    depositPaymentRequest: false,
    kycRequest: false,
    detectedBrokerNames: [],
    detectedCryptoAddresses: [],
    detectedAmountClaims: [],
    matchedKeywords: [],
    signalCount: 0,
  };

  const lowerText = text.toLowerCase();

  // SEBI reference detection
  for (const pattern of SEBI_PATTERNS) {
    if (pattern.test(text)) {
      signals.sebiReference = true;
      signals.matchedKeywords.push('sebi_reference');
      break;
    }
  }

  // Fake broker reference — broker name in suspicious context
  for (const broker of KNOWN_BROKER_NAMES) {
    if (lowerText.includes(broker)) {
      // Check if in suspicious context (reward, tip, guaranteed, etc.)
      const hasSuspiciousContext = /\b(?:guaranteed?|free|tip|signal|join|group|earn|profit|returns?|bonus|reward|deposit|transfer)\b/i.test(text);
      if (hasSuspiciousContext) {
        signals.fakeBrokerReference = true;
        signals.detectedBrokerNames.push(broker);
        signals.matchedKeywords.push('fake_broker_reference');
      }
      break;
    }
  }

  // Broker app link detection
  for (const keyword of FAKE_PLATFORM_KEYWORDS) {
    if (lowerText.includes(keyword)) {
      const hasLink = /https?:\/\/[^\s]+/i.test(text);
      const hasDownload = /\b(?:download|install|click|register|sign[\s-]*up)\b/i.test(lowerText);
      if (hasLink || hasDownload) {
        signals.brokerAppLink = true;
        signals.matchedKeywords.push('broker_app_link');
        break;
      }
    }
  }

  // Crypto wallet address detection
  for (const pattern of CRYPTO_ADDRESS_PATTERNS) {
    const match = text.match(pattern);
    if (match) {
      signals.cryptoWalletAddress = true;
      signals.detectedCryptoAddresses.push(match[0]);
      signals.matchedKeywords.push('crypto_wallet_address');
    }
  }

  // Guaranteed return claims
  for (const pattern of GUARANTEED_RETURN_PATTERNS) {
    const match = text.match(pattern);
    if (match) {
      signals.guaranteedReturnClaim = true;
      signals.detectedAmountClaims.push(match[0]);
      signals.matchedKeywords.push('guaranteed_return_claim');
      break;
    }
  }

  // IPO scam detection
  for (const pattern of IPO_SCAM_PATTERNS) {
    if (pattern.test(text)) {
      signals.fakeIpoAllotment = true;
      signals.matchedKeywords.push('fake_ipo_allotment');
      break;
    }
  }

  // Demat account solicitation
  for (const pattern of DEMAT_PATTERNS) {
    if (pattern.test(text)) {
      signals.dematAccountReference = true;
      signals.matchedKeywords.push('demat_account_reference');
      break;
    }
  }

  // Trading tip group
  for (const pattern of TIP_GROUP_PATTERNS) {
    if (pattern.test(text)) {
      signals.tradingTipGroup = true;
      signals.matchedKeywords.push('trading_tip_group');
      break;
    }
  }

  // KYC phishing
  for (const pattern of KYC_PHISHING_PATTERNS) {
    if (pattern.test(text)) {
      signals.kycRequest = true;
      signals.matchedKeywords.push('kyc_phishing');
      break;
    }
  }

  // Deposit/payment request
  for (const pattern of DEPOSIT_REQUEST_PATTERNS) {
    const match = text.match(pattern);
    if (match) {
      signals.depositPaymentRequest = true;
      signals.detectedAmountClaims.push(match[0]);
      signals.matchedKeywords.push('deposit_payment_request');
      break;
    }
  }

  // Calculate signal count and composite flag
  const booleanSignals = [
    signals.sebiReference,
    signals.fakeBrokerReference,
    signals.brokerAppLink,
    signals.dematAccountReference,
    signals.cryptoWalletAddress,
    signals.guaranteedReturnClaim,
    signals.fakeIpoAllotment,
    signals.tradingTipGroup,
    signals.depositPaymentRequest,
    signals.kycRequest,
  ];

  signals.signalCount = booleanSignals.filter(Boolean).length;
  signals.hasTradingFraudSignals = signals.signalCount > 0;

  return signals;
}
