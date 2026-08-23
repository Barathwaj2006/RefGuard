import { sanitizeText, sanitizeEvidenceData } from '../src/services/extractors/piiSanitizer';

describe('PII Sanitizer', () => {
  it('should mask Aadhaar numbers', () => {
    const text = 'My aadhaar is 1234-5678-9012 please check it.';
    const result = sanitizeText(text);
    expect(result.sanitizedText).toContain('[AADHAAR_REDACTED]');
    expect(result.hadPii).toBe(true);
    expect(result.redactions.some(r => r.type === 'AADHAAR')).toBe(true);
  });

  it('should mask PAN numbers', () => {
    const text = 'My PAN number is ABCDE1234F';
    const result = sanitizeText(text);
    expect(result.sanitizedText).toContain('[PAN_REDACTED]');
    expect(result.hadPii).toBe(true);
  });

  it('should mask phone numbers but leave last 4 digits', () => {
    const text = 'Call me at 9876543210 immediately.';
    const result = sanitizeText(text);
    expect(result.sanitizedText).toContain('XXXXXX3210');
    expect(result.hadPii).toBe(true);
  });

  it('should mask email addresses but preserve domain', () => {
    const text = 'Contact me at testuser@gmail.com';
    const result = sanitizeText(text);
    expect(result.sanitizedText).toContain('t***@gmail.com');
  });

  it('should mask OTP/PIN values', () => {
    const text = 'Your OTP is 123456. Do not share it.';
    const result = sanitizeText(text);
    expect(result.sanitizedText).toContain('[REDACTED]');
    expect(result.sanitizedText).not.toContain('123456');
  });

  it('should mask bank account numbers', () => {
    const text = 'Transfer to my a/c number 1234567890123';
    const result = sanitizeText(text);
    expect(result.sanitizedText).toContain('[ACCOUNT_REDACTED]');
  });

  it('should handle multiple PII types in same string', () => {
    const text = 'Please send OTP 12345 to phone 9876543210 or email test@test.com. PAN: ABCDE1234F';
    const result = sanitizeText(text);
    expect(result.hadPii).toBe(true);
    expect(result.redactions.length).toBe(4);
  });

  it('should sanitize evidence data correctly', () => {
    const text = 'Original: 9876543210';
    const sanitized = sanitizeEvidenceData(text);
    expect(sanitized).toBe('Original: XXXXXX3210');
  });
});
