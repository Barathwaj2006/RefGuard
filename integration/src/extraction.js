/**
 * RefGuard Universal Extraction Engine
 * Extracts URLs, VPAs, Referral IDs, Payment Parameters, Amounts, and Psychological Hooks.
 */

class ExtractionEngine {
  extract(scanRequest) {
    const rawContent = scanRequest.content_value || '';
    const contentType = scanRequest.content_type;
    const sourceContext = scanRequest.source_context || 'unknown';

    let textToAnalyze = rawContent;
    let isImageOrScreenshot = (contentType === 'IMAGE');
    let isQr = (contentType === 'QR');

    // If Base64 image simulation or encoded string
    if (isImageOrScreenshot && rawContent.length > 50) {
      try {
        const decoded = Buffer.from(rawContent, 'base64').toString('utf8');
        if (decoded && decoded.length > 5 && /^[\x20-\x7E\r\n\t]+$/.test(decoded)) {
          textToAnalyze = decoded;
        }
      } catch (e) {
        // Plain string fallback
      }
    }

    // 1. Extract UPI Pay URLs and parameters
    const upiPayload = this.extractUpi(textToAnalyze);

    // 2. Extract General URLs & Domains
    const urls = this.extractUrls(textToAnalyze);

    // 3. Extract VPAs
    const vpas = this.extractVpas(textToAnalyze, upiPayload);

    // 4. Extract Referral Codes
    const referralCodes = this.extractReferrals(textToAnalyze, urls);

    // 5. Extract Monetary Amounts
    const amounts = this.extractAmounts(textToAnalyze, upiPayload);

    // 6. Extract Behavioral/Scam keywords & Stated Intent
    const intentSignals = this.extractIntentSignals(textToAnalyze);

    return {
      contentType,
      sourceContext,
      rawContent,
      textAnalyzed: textToAnalyze,
      upi: upiPayload,
      urls,
      vpas,
      referralCodes,
      amounts,
      intentSignals,
      isQr,
      isImageOrScreenshot
    };
  }

  extractUpi(text) {
    const upiRegex = /upi:\/\/pay\?([^\s"'<>]+)/i;
    const match = text.match(upiRegex);
    if (!match) {
      return null;
    }
    const queryString = match[1];
    const params = new URLSearchParams(queryString);
    return {
      rawUri: match[0],
      pa: params.get('pa') || null,
      pn: params.get('pn') || null,
      am: params.get('am') ? parseFloat(params.get('am')) : null,
      cu: params.get('cu') || 'INR',
      tn: params.get('tn') || null,
      mc: params.get('mc') || null,
      tr: params.get('tr') || null
    };
  }

  extractUrls(text) {
    const urlRegex = /(https?:\/\/[^\s"'<>]+|www\.[^\s"'<>]+)/gi;
    const matches = text.match(urlRegex) || [];
    return matches.map(u => {
      let fullUrl = u.startsWith('http') ? u : 'https://' + u;
      let hostname = '';
      let pathname = '';
      try {
        const parsed = new URL(fullUrl);
        hostname = parsed.hostname.toLowerCase();
        pathname = parsed.pathname;
      } catch (e) {
        hostname = u.split('/')[0].toLowerCase();
      }
      return {
        raw: u,
        fullUrl,
        hostname,
        pathname,
        isShortLink: ['bit.ly', 'tinyurl.com', 't.me', 'wa.me', 'cutt.ly', 'is.gd', 'rb.gy'].includes(hostname)
      };
    });
  }

  extractVpas(text, upiPayload) {
    const vpas = new Set();
    if (upiPayload && upiPayload.pa) {
      vpas.add(upiPayload.pa.toLowerCase().trim());
    }

    const vpaRegex = /\b([a-zA-Z0-9.\-_]{2,64}@[a-zA-Z]{2,32})\b/g;
    let match;
    while ((match = vpaRegex.exec(text)) !== null) {
      const v = match[1].toLowerCase().trim();
      if (!v.endsWith('@gmail.com') && !v.endsWith('@yahoo.com') && !v.endsWith('@outlook.com') && !v.endsWith('@domain.com')) {
        vpas.add(v);
      } else if (v.includes('pay') || v.includes('upi')) {
        vpas.add(v);
      }
    }
    return Array.from(vpas);
  }

  extractReferrals(text, urls) {
    const refs = [];
    const refRegexes = [
      /(?:ref|referral|code|invite|invitation)[=_:\-\s]+([a-zA-Z0-9]{4,20})/gi,
      /[?&](?:ref|referral|code|invite)=([a-zA-Z0-9]+)/gi
    ];

    for (const r of refRegexes) {
      let match;
      while ((match = r.exec(text)) !== null) {
        refs.push(match[1]);
      }
    }

    for (const urlObj of urls) {
      try {
        const parsed = new URL(urlObj.fullUrl);
        const refVal = parsed.searchParams.get('ref') || parsed.searchParams.get('code') || parsed.searchParams.get('invite');
        if (refVal && !refs.includes(refVal)) {
          refs.push(refVal);
        }
      } catch (e) {}
    }

    return Array.from(new Set(refs));
  }

  extractAmounts(text, upiPayload) {
    const amounts = [];
    if (upiPayload && upiPayload.am) {
      amounts.push(upiPayload.am);
    }

    const amountRegex = /(?:₹|Rs\.?|INR|\bamount\b)\s*([\d,]+(?:\.\d{1,2})?)/gi;
    let match;
    while ((match = amountRegex.exec(text)) !== null) {
      const num = parseFloat(match[1].replace(/,/g, ''));
      if (!isNaN(num) && !amounts.includes(num)) {
        amounts.push(num);
      }
    }
    return amounts;
  }

  extractIntentSignals(text) {
    const lower = text.toLowerCase();
    const statedCredit = /(congratulations|you won|won|credited|receive|cashback|reward|claim reward|scratch card|lottery|prize|refund)/i.test(lower);
    const urgency = /(urgent|immediate|account blocked|kyc expired|within 24 hours|action required|limited offer|expires today)/i.test(lower);
    const pinPhishing = /(enter upi pin to receive|enter pin to credit|enter mpn|scan and enter pin)/i.test(lower);
    const taskScam = /(part time job|telegram task|youtube like task|daily income|investment doubling|vip level)/i.test(lower);
    const fakeAuthority = /(npci|cbi|rbi|income tax|customs department|electricity board bill|telecom verification|aadhaar|money laundering|digital arrest)/i.test(lower);

    return {
      statedCredit,
      urgency,
      pinPhishing,
      taskScam,
      fakeAuthority
    };
  }
}

module.exports = ExtractionEngine;
