export interface PaymentMismatchResult {
  isMismatch: boolean;
  statedIntent: string;
  actualAction: string;
  confidence: number;
}

export function extractPaymentMismatch(text: string, isCollectRequest: boolean): PaymentMismatchResult {
  const normalizedText = text.toLowerCase();

  // Look for keywords indicating the user is receiving something or verifying an account
  const inboundKeywords = [
    'refund', 'reward', 'cashback', 'lottery', 'prize', 'won', 'winner',
    'claim', 'receive', 'subsidy', 'credit', 'bonus', 'gift', 'lucky draw',
    'verify', 'verification', 'authenticate', 'activation'
  ];

  let detectedIntent = 'STANDARD_PAYMENT';
  let hasInboundClaim = false;

  for (const keyword of inboundKeywords) {
    const regex = new RegExp(`\\b${keyword}\\b`, 'i');
    if (regex.test(normalizedText)) {
      hasInboundClaim = true;
      if (['verify', 'verification', 'authenticate', 'activation'].includes(keyword)) {
        detectedIntent = 'ACCOUNT_VERIFICATION';
      } else {
        detectedIntent = 'RECEIVE_FUNDS_OR_PRIZE';
      }
      break; // Found primary intent
    }
  }

  const actualAction = isCollectRequest ? 'OUTBOUND_DEBIT_COLLECT' : 'NONE';

  // Mismatch happens when user believes they are receiving/verifying, but action is outbound debit
  const isMismatch = hasInboundClaim && isCollectRequest;

  const confidence = isMismatch ? 0.95 : (hasInboundClaim ? 0.8 : 0.6);

  return {
    isMismatch,
    statedIntent: hasInboundClaim ? detectedIntent : 'STANDARD_PAYMENT',
    actualAction,
    confidence
  };
}
