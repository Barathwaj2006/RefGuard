const fs = require('fs');
let html = fs.readFileSync('integration/demo/public/index.html', 'utf8');

const targetMain = html.substring(html.indexOf('<main class="container">'), html.indexOf('<!-- History View -->'));

const newMain = `<main class="app-container">
    
    <!-- SCANNER VIEW -->
    <div id="workspaceGrid">
      
      <!-- Presets (Hidden on Mobile typically, but let's keep them small) -->
      <section class="presets-section" id="presetsSection" style="margin-bottom: 1.5rem; display: flex; flex-wrap: wrap; gap: 0.5rem; justify-content: center;">
          <div class="preset-pill" onclick="loadPreset('upi_refund_trap')" style="background:var(--surface-main); border:1px solid var(--surface-border); padding:0.4rem 0.8rem; border-radius:100px; font-size:0.75rem; cursor:pointer; font-weight:600; box-shadow:var(--shadow-sm);">UPI Trap</div>
          <div class="preset-pill" onclick="loadPreset('digital_arrest_scam')" style="background:var(--surface-main); border:1px solid var(--surface-border); padding:0.4rem 0.8rem; border-radius:100px; font-size:0.75rem; cursor:pointer; font-weight:600; box-shadow:var(--shadow-sm);">Digital Arrest</div>
          <div class="preset-pill" onclick="loadPreset('legit_cdsl_alert')" style="background:var(--surface-main); border:1px solid var(--surface-border); padding:0.4rem 0.8rem; border-radius:100px; font-size:0.75rem; cursor:pointer; font-weight:600; box-shadow:var(--shadow-sm);">Legit CDSL</div>
          <div class="preset-pill" onclick="loadPreset('fake_referral')" style="background:var(--surface-main); border:1px solid var(--surface-border); padding:0.4rem 0.8rem; border-radius:100px; font-size:0.75rem; cursor:pointer; font-weight:600; box-shadow:var(--shadow-sm);">Fake Link</div>
      </section>

      <!-- Input Panel -->
      <section class="scanner-box" id="inputPanel">
        <div class="hero-header">
          <h2>CHECK ANY MESSAGE FOR SCAMS</h2>
          <p>Paste a suspicious message, email, payment request, or link.</p>
        </div>

        <div style="display:none;">
          <select id="contentTypeSelect"><option value="TEXT">TEXT</option></select>
          <select id="sourceContextSelect"><option value="Unknown" selected>Unknown</option></select>
        </div>

        <textarea id="contentValueArea" class="input-area" placeholder="Paste message, link, or content here..." maxlength="1500"></textarea>

        <div style="display:flex; gap:1rem;">
          <button id="scanBtn" class="primary-btn" onclick="executeScan()">
            CHECK FOR SCAM
          </button>
        </div>
        <div class="secure-badge">
          <span style="font-size:1.1rem;">🛡️</span> Your content is analyzed securely.
        </div>
      </section>

      <!-- Desktop Empty State -->
      <div id="desktopEmptyState" style="display:none;"></div>

      <section id="resultsPanel" style="display: none;">
        
        <!-- Loading State -->
        <div id="loadingState" class="loading-overlay" style="display: none;" aria-live="assertive" aria-atomic="true">
          <div class="spinner"></div>
          <h4 class="loading-text" id="loadingText">ANALYZING</h4>
          <p class="loading-subtext">Detecting scam signals & building threat profile...</p>
        </div>

        <!-- Error State -->
        <div id="errorState" class="loading-overlay" style="display: none;">
          <div class="verdict-icon">⚠️</div>
          <h4 class="loading-text">Unable to connect</h4>
          <p id="errorMessage" class="loading-subtext">Could not reach the protection engine.</p>
          <button class="primary-btn" onclick="executeScan()" style="margin-top: 1rem; width: auto; margin-inline: auto;">Try Again</button>
        </div>

        <!-- Result Content Container -->
        <div id="resultContent" style="display: none;" aria-live="polite">
          
          <!-- Verdict Hero -->
          <div id="riskBanner" class="verdict-hero">
            <div class="verdict-icon" id="verdictIcon">🛡️</div>
            <h2 class="verdict-title" id="severityTag">ANALYZING</h2>
            <div class="score-row">
              <div class="score-block">
                <span class="score-label">Risk Score</span>
                <span class="score-value"><span id="riskScoreVal">0</span><span style="font-size:1rem;opacity:0.5;">/100</span></span>
              </div>
              <div class="score-block" style="border-left: 2px solid rgba(0,0,0,0.1); padding-left: 2rem;">
                <span class="score-label">Confidence</span>
                <span class="score-value"><span id="riskConfidenceVal">0</span><span style="font-size:1rem;opacity:0.5;">%</span></span>
              </div>
            </div>
            <div id="heroImmediateAction" class="immediate-action" style="display: none;"></div>
          </div>
          
          <!-- Explanations (AI Analysis) -->
          <div id="explanationCard" class="result-card" style="display: none;">
            <h3 class="card-title">✨ WHY REFGUARD FLAGGED THIS</h3>
            <div id="dynamicSignalsContainer"></div>
          </div>

          <!-- Scam Chain -->
          <div id="scamChainCard" class="result-card" style="display: none;">
            <h3 class="card-title">🔍 WHAT IS HAPPENING?</h3>
            <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1.5rem;">Scam Chain</p>
            <div id="adaptiveChainContainer" class="chain-container"></div>
          </div>

          <!-- Adaptive Intelligence -->
          <div id="adaptiveIntelCard" class="result-card" style="display: none;">
            <h3 class="card-title">🎯 THREAT PROFILE</h3>
            <div class="intel-grid">
              
              <div class="intel-item" id="aiScamTypeBox">
                <div class="intel-icon" style="color:var(--color-critical); background:var(--color-critical-bg);">🎭</div>
                <div class="intel-content">
                  <div class="intel-label">Scam Type</div>
                  <div class="intel-value" id="aiScamType"></div>
                </div>
              </div>
              
              <div class="intel-item" id="aiStageBox">
                <div class="intel-icon" style="color:var(--accent-electric); background:rgba(0,102,255,0.1);">⏱️</div>
                <div class="intel-content">
                  <div class="intel-label">Current Stage</div>
                  <div class="intel-value" id="aiStage"></div>
                </div>
              </div>
              
              <div class="intel-item" id="aiObjectiveBox">
                <div class="intel-icon" style="color:var(--text-dark); background:var(--surface-border);">🎯</div>
                <div class="intel-content">
                  <div class="intel-label">Attacker Objective</div>
                  <div class="intel-value" id="aiObjective"></div>
                </div>
              </div>
              
              <div class="intel-item" id="aiRiskBox">
                <div class="intel-icon" style="color:var(--color-high); background:var(--color-high-bg);">⚠️</div>
                <div class="intel-content">
                  <div class="intel-label">User Risk</div>
                  <div class="intel-value" id="aiUserRisk"></div>
                </div>
              </div>
              
              <div class="intel-item" id="aiNextStepBox">
                <div class="intel-icon" style="color:var(--color-ai); background:var(--color-ai-bg);">🔮</div>
                <div class="intel-content">
                  <div class="intel-label">Next Likely Step</div>
                  <div class="intel-value" id="aiNextStep"></div>
                </div>
              </div>

              <div class="intel-item" id="aiRecommendedBox">
                <div class="intel-icon" style="color:var(--color-low); background:var(--color-low-bg);">🛡️</div>
                <div class="intel-content">
                  <div class="intel-label">Recommended Action</div>
                  <div class="intel-value" id="aiRecommendedAction"></div>
                </div>
              </div>

            </div>
          </div>

          <!-- Protect & Recover (Incident Response) -->
          <div id="incidentCard" class="result-card" style="display: none;">
            <h3 class="card-title">🛡️ PROTECT YOURSELF NOW</h3>
            
            <div id="incidentLoading" style="display:none; text-align:center; padding:2rem; color:var(--text-muted);">Preparing safety guidance...</div>
            <div id="incidentError" style="display:none; text-align:center; padding:2rem; color:var(--color-critical);">Failed to load guidance. <a href="#" id="retryIncidentBtn" class="tappable-link">Retry</a></div>
            
            <div id="incidentSafe" style="display:none;">
              <div class="action-list">
                <div class="action-item safe">
                  <div class="action-number">✓</div>
                  <div class="action-text">This message appears legitimate. No scam patterns detected. Stay alert, stay safe!</div>
                </div>
              </div>
            </div>

            <div id="incidentContent" style="display:none;">
              <div class="action-list">
                <div class="action-item critical">
                  <div class="action-number">01</div>
                  <div class="action-text" id="incImmediateAction"></div>
                </div>
                <div class="action-item">
                  <div class="action-number">02</div>
                  <div class="action-text" id="incPaymentAction"></div>
                </div>
                <div class="action-item">
                  <div class="action-number">03</div>
                  <div class="action-text" id="incEvidenceAction"></div>
                </div>
                <div class="action-item">
                  <div class="action-number">04</div>
                  <div class="action-text">Report: <span id="incReportingDest"></span></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Evidence Pack -->
          <details class="evidence-details" id="evidenceCard" style="display: none; margin-bottom: 2rem;">
            <summary>View Technical Evidence</summary>
            <div class="evidence-content" id="evidenceContainer"></div>
          </details>
          
        </div>
      </section>
    </div>

    `;

if (html.includes('<main class="container">')) {
  html = html.replace(targetMain, newMain);
  
  // Also clean up Bottom Nav
  const targetNav = html.substring(html.indexOf('<!-- Bottom Navigation -->'), html.indexOf('</body>'));
  const newNav = `<!-- Bottom Navigation -->
  <nav class="bottom-nav">
    <button class="nav-item active" onclick="switchTab('scan', this)">
      <span style="font-size:1.25rem;">🔍</span>
      <span>Scan</span>
    </button>
    <button class="nav-item" onclick="switchTab('history', this)">
      <span style="font-size:1.25rem;">🕒</span>
      <span>History</span>
    </button>
    <button class="nav-item" onclick="switchTab('intel', this)">
      <span style="font-size:1.25rem;">🌐</span>
      <span>Report</span>
    </button>
  </nav>

  <script src="app.js"></script>
`;
  html = html.replace(targetNav, newNav);

  // Clean up History/Intel views headers to match styling
  html = html.replace('<h2>Scan History</h2>', '<h2 style="font-size:1.5rem;font-weight:800;color:var(--bg-navy);margin-bottom:1.5rem;text-align:center;">SCAN HISTORY</h2>');
  html = html.replace('<h2>Global Intelligence</h2>', '<h2 style="font-size:1.5rem;font-weight:800;color:var(--bg-navy);margin-bottom:1.5rem;text-align:center;">REPORT & INTEL</h2>');
  
  // Convert Report form to beautiful card
  const reportFormTarget = html.substring(html.indexOf('<div class="report-form-card">'), html.indexOf('<!-- Global Threat Feed -->'));
  const newReportForm = `<div class="result-card">
          <h3 class="card-title">REPORT SCAM</h3>
          <p style="font-size:0.9rem;color:var(--text-muted);margin-bottom:1.5rem;">Help protect others by reporting suspicious messages.</p>
          
          <div style="margin-bottom: 1rem;">
            <label style="display:block;font-weight:700;margin-bottom:0.5rem;font-size:0.9rem;">Suspicious Content</label>
            <textarea id="reportIndicator" class="input-area" style="height:100px;" placeholder="Paste the exact message or link here..."></textarea>
          </div>
          
          <div style="margin-bottom: 1rem;">
            <label style="display:block;font-weight:700;margin-bottom:0.5rem;font-size:0.9rem;">Category</label>
            <select id="reportCategory" class="input-area" style="height:48px;padding:0.5rem;">
              <option value="COMMUNITY_REPORT">Spam / Scam Message</option>
              <option value="PAYMENT_FRAUD">UPI / Payment Request</option>
              <option value="IMPERSONATION">Fake Bank / Gov Alert</option>
            </select>
          </div>
          
          <div style="margin-bottom: 1.5rem;">
            <label style="display:block;font-weight:700;margin-bottom:0.5rem;font-size:0.9rem;">Description (Optional)</label>
            <textarea id="reportDesc" class="input-area" style="height:80px;" placeholder="Any additional context..."></textarea>
          </div>
          
          <button id="submitReportBtn" class="primary-btn" onclick="submitUserReport()">
            SUBMIT REPORT
          </button>
          
          <div id="reportSuccess" style="display: none; margin-top: 1rem; color: var(--color-low); text-align: center; font-weight: 700; background: var(--color-low-bg); padding: 1rem; border-radius: var(--radius-md);">
            ✓ Report submitted successfully. Thank you!
          </div>
        </div>
        
        `;
  if(reportFormTarget.includes('report-form-card')){
    html = html.replace(reportFormTarget, newReportForm);
  }

  fs.writeFileSync('integration/demo/public/index.html', html);
}
