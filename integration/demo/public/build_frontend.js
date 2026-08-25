const fs = require('fs');

// 1. PATCH index.html
let html = fs.readFileSync('integration/demo/public/index.html', 'utf8');

// A. Privacy Text
const scanBtnStr = `<button id="scanBtn" class="primary-btn" onclick="executeScan()">
          CHECK FOR SCAM
        </button>`;
if (!html.includes('RefGuard analyzes suspicious content')) {
  html = html.replace(scanBtnStr, scanBtnStr + `
        <p style="font-size: 0.75rem; color: var(--text-muted); text-align: center; margin-top: 1rem; line-height: 1.4;">
          🔒 RefGuard analyzes suspicious content to identify scam signals. Sensitive personal information is protected before processing.
        </p>`);
}

// B. Scam Reporting Tab & Modal
if (!html.includes('id="reportView"')) {
  // Add Nav button
  const navIntelStr = `<button class="nav-btn" id="navIntel" onclick="switchTab('intel')">
      <span class="nav-icon">🌍</span>
      <span>Intel</span>
    </button>`;
  html = html.replace(navIntelStr, navIntelStr + `
    <button class="nav-btn" id="navReport" onclick="switchTab('report')">
      <span class="nav-icon">📢</span>
      <span>Report</span>
    </button>`);

  // Add Report View container
  const intelViewEnd = `      </div>
    </div>`; // End of intel view roughly
    
  const reportHTML = `

    <!-- REPORT VIEW -->
    <div class="scanner-container" id="reportView" style="display: none;">
      <div class="input-header">
        <h2>Report a Scam</h2>
        <p>Help protect the community by reporting fraudulent messages or links.</p>
      </div>
      
      <div class="input-card" style="margin-top: 0;">
        <div class="form-group">
          <label for="reportIndicator">Suspicious Content (URL, Message, or UPI ID)</label>
          <textarea id="reportIndicator" placeholder="Paste the exact message or link..." style="min-height: 80px;"></textarea>
        </div>
        <div class="form-group">
          <label for="reportCategory">Category</label>
          <select id="reportCategory">
            <option value="COMMUNITY_REPORT">General Suspicious Activity</option>
            <option value="UPI_FRAUD">UPI / Payment Fraud</option>
            <option value="IMPERSONATION">Authority Impersonation</option>
            <option value="PHISHING">Phishing Link</option>
          </select>
        </div>
        <div class="form-group">
          <label for="reportDesc">Additional Details (Optional)</label>
          <textarea id="reportDesc" placeholder="How did you encounter this?" style="min-height: 60px;"></textarea>
        </div>
        <p style="font-size: 0.75rem; color: var(--text-muted); margin-bottom: 1rem;">
          Your report will be analyzed and added to the Global Intel registry if verified.
        </p>
        <button id="submitReportBtn" class="primary-btn" onclick="submitUserReport()">
          SUBMIT REPORT
        </button>
        <div id="reportSuccess" style="display: none; margin-top: 1rem; color: var(--color-low); text-align: center; font-weight: 600;">
          ✅ Report submitted successfully. Thank you!
        </div>
      </div>
    </div>
  `;
  
  html = html.replace('<!-- INTEL VIEW -->', reportHTML + '\n    <!-- INTEL VIEW -->');
}

// C. Signals Container in Explanation Card
if (!html.includes('id="dynamicSignalsContainer"')) {
  const signalGridStart = `<div class="signal-grid" id="signalGrid">`;
  html = html.replace(signalGridStart, signalGridStart + `
              <!-- Friendly Signal Cards -->
              <div id="dynamicSignalsContainer" style="display: contents;"></div>`);
}

fs.writeFileSync('integration/demo/public/index.html', html);


// 2. PATCH app.js
let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

// Tab logic patch
if (!app.includes(`tab === 'report'`)) {
  app = app.replace(
    /const navIntel = document.getElementById\('navIntel'\);/,
    `const navIntel = document.getElementById('navIntel');
  const navReport = document.getElementById('navReport');`
  );
  app = app.replace(
    /const intelView = document.getElementById\('intelView'\);/,
    `const intelView = document.getElementById('intelView');
  const reportView = document.getElementById('reportView');`
  );
  app = app.replace(
    /if \(navIntel\) navIntel.classList.remove\('active'\);/,
    `if (navIntel) navIntel.classList.remove('active');
  if (navReport) navReport.classList.remove('active');`
  );
  app = app.replace(
    /if \(intelView\) intelView.style.display = 'none';/,
    `if (intelView) intelView.style.display = 'none';
  if (reportView) reportView.style.display = 'none';`
  );
  
  app = app.replace(
    /  } else if \(tab === 'intel'\) \{[\s\S]*?loadIntel\(\);\n  \}/,
    `  } else if (tab === 'intel') {
    if (navIntel) navIntel.classList.add('active');
    if (intelView) intelView.style.display = 'block';
    loadIntel();
  } else if (tab === 'report') {
    if (navReport) navReport.classList.add('active');
    if (reportView) reportView.style.display = 'block';
  }`
  );
}

// New Report function
if (!app.includes('async function submitUserReport()')) {
  const newReportFn = `
async function submitUserReport() {
  const indicatorEl = document.getElementById('reportIndicator');
  const catEl = document.getElementById('reportCategory');
  const descEl = document.getElementById('reportDesc');
  const btn = document.getElementById('submitReportBtn');
  const successMsg = document.getElementById('reportSuccess');

  const indicator = indicatorEl ? indicatorEl.value.trim() : '';
  if (!indicator) {
    showToast('Please provide the suspicious content to report.', 'error');
    return;
  }

  if (btn) {
    btn.disabled = true;
    btn.innerText = 'SUBMITTING...';
  }
  if (successMsg) successMsg.style.display = 'none';

  const reportPayload = {
    report_id: 'rep_' + Math.random().toString(36).substring(2, 10),
    reported_indicator: indicator.substring(0, 500),
    report_category: catEl ? catEl.value : 'COMMUNITY_REPORT',
    description: descEl ? descEl.value.substring(0, 500) : '',
    submission_timestamp: new Date().toISOString(),
    moderation_status: 'PENDING',
    confidence: 0.85,
    provenance: 'USER_SUBMISSION'
  };

  try {
    const res = await fetch('/api/v1/report', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(reportPayload)
    });

    if (!res.ok) {
      throw new Error('Report rejected by server');
    }

    if (successMsg) successMsg.style.display = 'block';
    if (indicatorEl) indicatorEl.value = '';
    if (descEl) descEl.value = '';
    
    // Switch to intel to see it potentially (if we supported instant optimistic updates, we would do it here)
    setTimeout(() => {
      if (successMsg) successMsg.style.display = 'none';
    }, 4000);

  } catch (err) {
    showToast('Failed to submit report: ' + err.message, 'error');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerText = 'SUBMIT REPORT';
    }
  }
}
`;
  app += newReportFn;
}

// Friendly Signal Cards
if (!app.includes('friendlySignalMap')) {
  // We'll replace the AI Explanation mapping logic.
  const oldAI = `  // AI Explanation & Mismatch
  const explanationCard = document.getElementById('explanationCard');
  const detectedSummary = document.getElementById('detectedSummary');
  const whyItMatters = document.getElementById('whyItMatters');
  
  if (explanationCard) {
    explanationCard.style.display = (severity === 'LOW') ? 'none' : 'block';
  }
  if (detectedSummary) detectedSummary.innerText = decision.detected_summary || risk.human_explanation || '';
  if (whyItMatters) whyItMatters.innerText = decision.why_it_matters || decision.user_instruction || '';`;

  const newAI = `  // AI Explanation & Signals
  const explanationCard = document.getElementById('explanationCard');
  const detectedSummary = document.getElementById('detectedSummary');
  const whyItMatters = document.getElementById('whyItMatters');
  const dynamicSignalsContainer = document.getElementById('dynamicSignalsContainer');
  
  if (explanationCard) {
    explanationCard.style.display = (severity === 'LOW') ? 'none' : 'block';
  }
  
  if (detectedSummary) detectedSummary.innerText = decision.detected_summary || risk.human_explanation || '';
  if (whyItMatters) whyItMatters.innerText = decision.why_it_matters || decision.user_instruction || '';

  if (dynamicSignalsContainer) {
    dynamicSignalsContainer.innerHTML = '';
    const friendlySignalMap = {
      'urgency_indicator': { title: 'Urgency', desc: 'The message pressures you to act immediately without thinking.' },
      'authority_impersonation': { title: 'Authority Impersonation', desc: 'The sender claims to represent an official organization.' },
      'sms_authority_impersonation': { title: 'Authority Impersonation', desc: 'The sender claims to represent an official organization.' },
      'digital_arrest_scam': { title: 'Digital Arrest Threat', desc: 'Uses fear tactics claiming illegal activity to demand money.' },
      'financial_reward': { title: 'Fake Reward', desc: 'Promises an unexpected reward or refund to trick you into paying.' },
      'upi_fraud_pattern': { title: 'UPI Collect Fraud', desc: 'Disguises a payment request as a refund or prize receipt.' }
    };

    const signals = risk.signals || [];
    signals.forEach(sig => {
      if (friendlySignalMap[sig]) {
        const div = document.createElement('div');
        div.className = 'signal-item';
        div.innerHTML = \`<div class="signal-title">⚠ \${friendlySignalMap[sig].title}</div>
                         <div class="signal-desc">\${friendlySignalMap[sig].desc}</div>\`;
        dynamicSignalsContainer.appendChild(div);
      }
    });
  }`;
  app = app.replace(oldAI, newAI);
}

fs.writeFileSync('integration/demo/public/app.js', app);
