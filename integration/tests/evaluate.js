const fs = require('fs');
const path = require('path');
const RefGuardPipeline = require('../src/pipeline');

const pipeline = new RefGuardPipeline();

const TEST_CASES = [
  // Malicious Inputs
  {
    type: 'malicious',
    payload: { content_type: 'URL', content_value: 'http://free-cashback-loot.xyz/claim?ref=998877', source_context: 'com.whatsapp', timestamp: new Date().toISOString() },
    expectedAction: ['REQUIRE_CONFIRMATION', 'DISCOURAGE_PROCEED'],
    expectedRisk: ['HIGH', 'CRITICAL']
  },
  {
    type: 'malicious',
    payload: { content_type: 'TEXT', content_value: 'CBI Alert: Your Aadhaar is linked to illegal money laundering. Call immediately or face digital arrest.', source_context: 'com.whatsapp', timestamp: new Date().toISOString() },
    expectedAction: ['REQUIRE_CONFIRMATION', 'DISCOURAGE_PROCEED'],
    expectedRisk: ['HIGH', 'CRITICAL']
  },

  // Legitimate Inputs
  {
    type: 'legitimate',
    payload: { content_type: 'QR', content_value: 'upi://pay?pa=swiggy@icici&pn=SwiggyOrders&am=350&cu=INR', source_context: 'com.swiggy.consumer', timestamp: new Date().toISOString() },
    expectedAction: ['ALLOW'],
    expectedRisk: ['LOW']
  },
  {
    type: 'legitimate',
    payload: { content_type: 'TEXT', content_value: 'Hey, are we still meeting for lunch at 1 PM?', source_context: 'com.whatsapp', timestamp: new Date().toISOString() },
    expectedAction: ['ALLOW'],
    expectedRisk: ['LOW']
  },

  // Ambiguous Inputs (Legitimate but weird)
  {
    type: 'ambiguous',
    payload: { content_type: 'TEXT', content_value: 'Your electricity bill of Rs.1200 is due on 15-Sep. Pay via official app to avoid late fees. BESCOM', source_context: 'com.android.mms', timestamp: new Date().toISOString() },
    expectedAction: ['ALLOW', 'REQUIRE_CONFIRMATION'],
    expectedRisk: ['LOW', 'MEDIUM']
  },

  // Payment Intent Mismatch
  {
    type: 'intent_mismatch',
    payload: { content_type: 'TEXT', content_value: 'Congratulations! You won 5000 cashback scratch card. Enter UPI PIN to claim: upi://pay?pa=rewards.collect@ybl&am=5000', source_context: 'com.whatsapp', timestamp: new Date().toISOString() },
    expectedAction: ['DISCOURAGE_PROCEED'],
    expectedRisk: ['CRITICAL']
  },

  // QR
  {
    type: 'qr',
    payload: { content_type: 'QR', content_value: 'upi://pay?pa=scammer@oksbi&pn=RewardClaim&am=2500&cu=INR', source_context: 'com.google.android.apps.nbu.paisa.user', timestamp: new Date().toISOString() },
    expectedAction: ['DISCOURAGE_PROCEED'],
    expectedRisk: ['CRITICAL']
  },

  // OCR/Screenshots
  {
    type: 'ocr',
    payload: { content_type: 'IMAGE', content_value: Buffer.from('Telegram Task Earning VIP: Earn 5000 daily by liking videos. Contact wa.me/919876543210 ref=TASK99').toString('base64'), source_context: 'com.android.gallery', timestamp: new Date().toISOString() },
    expectedAction: ['REQUIRE_CONFIRMATION', 'DISCOURAGE_PROCEED'],
    expectedRisk: ['HIGH', 'CRITICAL']
  },

  // URL/Referral
  {
    type: 'url_referral',
    payload: { content_type: 'URL', content_value: 'https://tinyurl.com/free-money-now', source_context: 'com.twitter.android', timestamp: new Date().toISOString() },
    expectedAction: ['REQUIRE_CONFIRMATION', 'DISCOURAGE_PROCEED'],
    expectedRisk: ['HIGH', 'CRITICAL']
  },

  // VPA
  {
    type: 'vpa',
    payload: { content_type: 'UPI_VPA', content_value: 'lottery.winner@paytm', source_context: 'com.refguard.manual', timestamp: new Date().toISOString() },
    expectedAction: ['DISCOURAGE_PROCEED'],
    expectedRisk: ['CRITICAL']
  },

  // Malformed
  {
    type: 'malformed',
    payload: { content_type: 'INVALID_TYPE', timestamp: new Date().toISOString() },
    expectedAction: ['ERROR'],
    expectedRisk: []
  },

  // Degraded conditions (no source context)
  {
    type: 'degraded',
    payload: { content_type: 'TEXT', content_value: 'Join my Telegram VIP group for guaranteed crypto returns!', source_context: '', timestamp: new Date().toISOString() },
    expectedAction: ['REQUIRE_CONFIRMATION', 'DISCOURAGE_PROCEED'],
    expectedRisk: ['HIGH', 'CRITICAL']
  }
];

let stats = {
  total: 0,
  passed: 0,
  failed: 0,
  falsePositives: 0, // Legitimate flagged as malicious
  falseNegatives: 0, // Malicious flagged as legitimate
  errors: 0
};

let results = [];

console.log("Running RefGuard Evaluation Pipeline...\n");

for (const tc of TEST_CASES) {
  stats.total++;
  try {
    const res = pipeline.processScan(tc.payload);
    const action = res.protection_decision.action;
    const risk = res.risk_assessment.risk_severity;

    let passed = true;
    if (!tc.expectedAction.includes(action)) {
       passed = false;
    }
    if (!tc.expectedRisk.includes(risk)) {
       passed = false;
    }

    if (passed) {
       stats.passed++;
    } else {
       stats.failed++;
       if (tc.type === 'legitimate' && risk !== 'LOW') stats.falsePositives++;
       if (tc.type !== 'legitimate' && tc.type !== 'ambiguous' && tc.type !== 'malformed' && risk === 'LOW') stats.falseNegatives++;
    }

    results.push({
      type: tc.type,
      passed,
      actualAction: action,
      actualRisk: risk,
      expectedAction: tc.expectedAction,
      expectedRisk: tc.expectedRisk,
      content: tc.payload.content_value ? tc.payload.content_value.substring(0, 50) + '...' : 'N/A'
    });

  } catch (err) {
    if (tc.expectedAction.includes('ERROR')) {
      stats.passed++;
      results.push({
        type: tc.type,
        passed: true,
        actualAction: 'ERROR (Caught)',
        actualRisk: 'N/A',
        expectedAction: tc.expectedAction,
        expectedRisk: tc.expectedRisk,
        content: tc.payload.content_value ? tc.payload.content_value.substring(0, 50) + '...' : 'N/A'
      });
    } else {
      stats.errors++;
      results.push({
        type: tc.type,
        passed: false,
        error: err.message,
        content: tc.payload.content_value ? tc.payload.content_value.substring(0, 50) + '...' : 'N/A'
      });
    }
  }
}

// Generate Report
const reportPath = path.join(__dirname, 'evaluation_report.md');

let report = `# RefGuard Evaluation Report\n\n`;
report += `## Summary\n`;
report += `- Total Tests: ${stats.total}\n`;
report += `- Passed: ${stats.passed}\n`;
report += `- Failed: ${stats.failed}\n`;
report += `- Errors: ${stats.errors}\n`;
report += `- False Positives (Legitimate flagged as bad): ${stats.falsePositives}\n`;
report += `- False Negatives (Malicious flagged as good): ${stats.falseNegatives}\n\n`;

report += `## Detailed Results\n`;
report += `| Type | Passed | Action (Actual/Expected) | Risk (Actual/Expected) | Content |\n`;
report += `|---|---|---|---|---|\n`;

results.forEach(r => {
  if (r.error) {
     report += `| ${r.type} | ❌ | ERROR | N/A | ${r.error} |\n`;
  } else {
     report += `| ${r.type} | ${r.passed ? '✅' : '❌'} | ${r.actualAction} / ${r.expectedAction.join(',')} | ${r.actualRisk} / ${r.expectedRisk.join(',')} | ${r.content.replace(/\|/g, '\\|')} |\n`;
  }
});

fs.writeFileSync(reportPath, report);
console.log(`Evaluation complete. Report generated at ${reportPath}`);

// Log to console as well
console.log(report);
