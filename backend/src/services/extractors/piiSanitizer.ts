/**
 * PII Sanitizer — Privacy-preserving preprocessor for RefGuard.
 *
 * Redacts PII (Aadhaar, PAN, phone numbers, email addresses, OTP/PIN values)
 * from content before it is sent to external services (Gemini) or stored
 * in evidence packs. Returns both sanitized text and a redaction manifest
 * for audit traceability.
 */

export interface RedactionEntry {
  type: 'AADHAAR' | 'PAN' | 'PHONE' | 'EMAIL' | 'OTP_PIN' | 'BANK_ACCOUNT';
  originalFragment: string;
  redactedFragment: string;
  position: number;
}

export interface SanitizationResult {
  sanitizedText: string;
  redactions: RedactionEntry[];
  hadPii: boolean;
}

// Aadhaar: 12 digits, optionally separated by spaces or dashes
const AADHAAR_PATTERN = /\b(\d{4}[\s-]?\d{4}[\s-]?\d{4})\b/g;

// PAN: 5 letters, 4 digits, 1 letter (e.g., ABCDE1234F)
const PAN_PATTERN = /\b([A-Z]{5}\d{4}[A-Z])\b/g;

// Indian phone numbers: optional +91/91/0 prefix followed by 10 digits
const PHONE_PATTERN = /\b(?:\+?91[\s-]?|0)?([6-9]\d{9})\b/g;

// Email addresses
const EMAIL_PATTERN = /\b([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\b/g;

// OTP/PIN values: 4-8 digit numbers preceded by OTP/PIN context
const OTP_PIN_PATTERN = /\b(?:OTP|otp|pin|PIN|code)\s*(?:is|:)?\s*(\d{4,8})\b/g;

// Bank account numbers: 9-18 digit sequences in account context
const BANK_ACCOUNT_PATTERN = /\b(?:a\/c|account|acct)\s*(?:no|number|#|:)?\s*(\d{9,18})\b/ig;

/**
 * Sanitize text by redacting PII patterns.
 * Returns sanitized text and a manifest of what was redacted.
 */
export function sanitizeText(text: string): SanitizationResult {
  const redactions: RedactionEntry[] = [];
  let sanitized = text;

  // Order matters: process longer/more specific patterns first to avoid
  // partial matches on overlapping patterns.

  // Aadhaar numbers
  sanitized = sanitized.replace(AADHAAR_PATTERN, (match, _digits, offset) => {
    // Verify it's actually 12 digits (not a subset of a longer number)
    const digitsOnly = match.replace(/[\s-]/g, '');
    if (digitsOnly.length !== 12) return match;

    const redacted = '[AADHAAR_REDACTED]';
    redactions.push({
      type: 'AADHAAR',
      originalFragment: match.slice(0, 4) + '****' + match.slice(-4),
      redactedFragment: redacted,
      position: offset,
    });
    return redacted;
  });

  // PAN numbers
  sanitized = sanitized.replace(PAN_PATTERN, (match, _pan, offset) => {
    const redacted = '[PAN_REDACTED]';
    redactions.push({
      type: 'PAN',
      originalFragment: match.slice(0, 2) + '***' + match.slice(-2),
      redactedFragment: redacted,
      position: offset,
    });
    return redacted;
  });

  // Bank account numbers (before phone to avoid false positives)
  sanitized = sanitized.replace(BANK_ACCOUNT_PATTERN, (match, digits, offset) => {
    const redacted = match.replace(digits, '[ACCOUNT_REDACTED]');
    redactions.push({
      type: 'BANK_ACCOUNT',
      originalFragment: '****' + digits.slice(-4),
      redactedFragment: redacted,
      position: offset,
    });
    return redacted;
  });

  // OTP/PIN values
  sanitized = sanitized.replace(OTP_PIN_PATTERN, (match, digits, offset) => {
    const redacted = match.replace(digits, '[REDACTED]');
    redactions.push({
      type: 'OTP_PIN',
      originalFragment: '****',
      redactedFragment: redacted,
      position: offset,
    });
    return redacted;
  });

  // Phone numbers — preserve last 4 digits for context
  sanitized = sanitized.replace(PHONE_PATTERN, (match, digits, offset) => {
    const last4 = digits.slice(-4);
    const redacted = match.replace(digits, 'XXXXXX' + last4);
    redactions.push({
      type: 'PHONE',
      originalFragment: 'XXXXXX' + last4,
      redactedFragment: redacted,
      position: offset,
    });
    return redacted;
  });

  // Email addresses — preserve domain for context
  sanitized = sanitized.replace(EMAIL_PATTERN, (match, local, domain, offset) => {
    const maskedLocal = local.charAt(0) + '***';
    const redacted = maskedLocal + '@' + domain;
    redactions.push({
      type: 'EMAIL',
      originalFragment: maskedLocal + '@' + domain,
      redactedFragment: redacted,
      position: offset,
    });
    return redacted;
  });

  return {
    sanitizedText: sanitized,
    redactions,
    hadPii: redactions.length > 0,
  };
}

/**
 * Sanitize evidence pack data values to strip PII before storage or external transmission.
 * Returns a new sanitized string (does not modify input).
 */
export function sanitizeEvidenceData(data: string): string {
  return sanitizeText(data).sanitizedText;
}
