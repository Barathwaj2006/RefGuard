/**
 * RefGuard Threat Intelligence & Local Community Registry
 * Evaluates entity reputation, domain risk, community report matches, and known scam patterns.
 */

class ThreatIntelligence {
  constructor() {
    this.knownBadDomains = new Set([
      'free-cashback-loot.xyz',
      'gpay-rewards-claim.top',
      'phonepe-reward-scratch.club',
      'paytm-kyc-verify.work',
      't.me/task_earning_vip',
      'win-daily-cash.click',
      'instant-loan-approval.xyz',
      'electricity-bill-update.online'
    ]);

    this.knownBadVpas = new Set([
      'scammer@oksbi',
      'fraudster@paytm',
      'lottery.winner@paytm',
      'cashback.claim@ibl',
      'rewards.collect@ybl',
      'fake.refund@okhdfcbank'
    ]);

    this.trustedMerchants = new Set([
      'swiggy@icici',
      'zomato@hdfcbank',
      'flipkart@axisbank',
      'amazonpay@apl',
      'uber@icici',
      'merchant@okhdfcbank'
    ]);

    this.communityReports = [];
    this.communityVpaCounts = new Map();
    this.communityDomainCounts = new Map();
  }

  addCommunityReport(report) {
    this.communityReports.push(report);
    const textToScan = ((report.reported_indicator || '') + ' ' + (report.description || '')).toLowerCase();
    const vpaMatches = textToScan.match(/[a-zA-Z0-9.\-_]+@[a-zA-Z]+/g) || [];
    for (const v of vpaMatches) {
      const vpa = v.trim();
      this.communityVpaCounts.set(vpa, (this.communityVpaCounts.get(vpa) || 0) + 1);
    }
    const domainMatches = textToScan.match(/[a-zA-Z0-9.\-_]+\.[a-zA-Z]{2,}/g) || [];
    for (const d of domainMatches) {
      if (!d.includes('@')) {
        const domain = d.trim();
        this.communityDomainCounts.set(domain, (this.communityDomainCounts.get(domain) || 0) + 1);
      }
    }
  }

  evaluate(extractedData) {
    const threats = [];
    let threatScore = 0;
    let threatCategory = 'UNKNOWN';

    // 1. Check Domains
    for (const u of extractedData.urls) {
      const domainHost = u.hostname;
      if (this.knownBadDomains.has(domainHost)) {
        threats.push({
          type: 'KNOWN_MALICIOUS_DOMAIN',
          source: 'LOCAL_THREAT_DATABASE',
          target: domainHost,
          confidence: 0.98,
          description: 'Domain ' + domainHost + ' is a known active phishing / scam domain.'
        });
        threatScore += 80;
        threatCategory = 'PHISHING_URL';
      } else if (this.communityDomainCounts.has(domainHost)) {
        const count = this.communityDomainCounts.get(domainHost);
        const isHighConfidence = count >= 3;
        threats.push({
          type: 'REPORTED_SUSPICIOUS_DOMAIN',
          source: 'COMMUNITY_REGISTRY',
          target: domainHost,
          confidence: isHighConfidence ? 0.85 : 0.60,
          description: `Domain ${domainHost} has been flagged in ${count} local community report(s) (${isHighConfidence ? 'Moderate' : 'Low'} confidence, requires corroboration).`
        });
        threatScore += isHighConfidence ? 80 : 40;
        threatCategory = 'PHISHING_URL';
      } else if (/\.(xyz|top|club|work|click|online|shop)$/i.test(domainHost)) {
        threats.push({
          type: 'HIGH_RISK_TLD',
          source: 'HEURISTIC_ENGINE',
          target: domainHost,
          confidence: 0.75,
          description: 'Domain ' + domainHost + ' uses a high-risk TLD commonly associated with disposable scam landing pages.'
        });
        threatScore += 35;
        if (threatCategory === 'UNKNOWN') threatCategory = 'PHISHING_URL';
      }

      if (/(gpay|phonepe|paytm|bhim|sbi|hdfc|icici).*(reward|cashback|bonus|loot|kyc)/i.test(domainHost)) {
        threats.push({
          type: 'BRAND_IMPERSONATION',
          source: 'HEURISTIC_ENGINE',
          target: domainHost,
          confidence: 0.92,
          description: 'Domain ' + domainHost + ' impersonates official payment provider brand.'
        });
        threatScore += 60;
        threatCategory = 'IMPERSONATION';
      }
    }

    // 2. Check VPAs
    for (const vpa of extractedData.vpas) {
      if (this.knownBadVpas.has(vpa)) {
        threats.push({
          type: 'REPORTED_FRAUDULENT_VPA',
          source: 'COMMUNITY_REGISTRY',
          target: vpa,
          confidence: 0.95,
          description: 'UPI ID ' + vpa + ' has been flagged in community scam reports.'
        });
        threatScore += 85;
        threatCategory = 'UNAUTHORIZED_COLLECT';
      } else if (this.communityVpaCounts.has(vpa)) {
        const count = this.communityVpaCounts.get(vpa);
        const isHighConfidence = count >= 3;
        threats.push({
          type: 'REPORTED_FRAUDULENT_VPA',
          source: 'COMMUNITY_REGISTRY',
          target: vpa,
          confidence: isHighConfidence ? 0.85 : 0.60,
          description: `UPI ID ${vpa} has been flagged in ${count} local community report(s) (${isHighConfidence ? 'Moderate' : 'Low'} confidence, requires corroboration).`
        });
        threatScore += isHighConfidence ? 85 : 45;
        threatCategory = 'UNAUTHORIZED_COLLECT';
      } else if (this.trustedMerchants.has(vpa)) {
        threats.push({
          type: 'VERIFIED_MERCHANT',
          source: 'CURATED_WHITELIST',
          target: vpa,
          confidence: 0.99,
          description: 'UPI ID ' + vpa + ' is an authentic verified merchant handle.'
        });
        threatScore -= 40;
      } else if (/(reward|cashback|refund|loot|bonus|lottery|winner)@/i.test(vpa)) {
        threats.push({
          type: 'DECEPTIVE_VPA_HANDLE',
          source: 'HEURISTIC_ENGINE',
          target: vpa,
          confidence: 0.85,
          description: 'VPA ' + vpa + ' uses deceptive keywords to trick users into believing it is a reward system.'
        });
        threatScore += 50;
        if (threatCategory === 'UNKNOWN') threatCategory = 'IMPERSONATION';
      }
    }

    // 3. Check Intent Signals
    if (extractedData.intentSignals.pinPhishing) {
      threats.push({
        type: 'UPI_PIN_PHISHING',
        source: 'PSYCHOLOGICAL_ANALYZER',
        target: 'CONTENT',
        confidence: 0.99,
        description: 'Explicit prompt asking user to enter UPI PIN to receive money (UPI PIN is ONLY for paying/debiting).'
      });
      threatScore += 95;
      threatCategory = 'UNAUTHORIZED_COLLECT';
    }

    if (extractedData.intentSignals.taskScam) {
      threats.push({
        type: 'TASK_INVESTMENT_FRAUD',
        source: 'BEHAVIORAL_RULES',
        target: 'CONTENT',
        confidence: 0.88,
        description: 'Telegram / Task completion earning scheme with high probability of advance-fee scam.'
      });
      threatScore += 70;
      threatCategory = 'FAKE_REFERRAL';
    }

    if (extractedData.referralCodes.length > 0 && threatScore > 30) {
      threatCategory = 'FAKE_REFERRAL';
    }

    return {
      threats,
      threatScore: Math.max(0, Math.min(100, threatScore)),
      threatCategory
    };
  }
}

module.exports = ThreatIntelligence;
