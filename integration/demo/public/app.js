// RefGuard — Core Web Application Logic
const HISTORY_STORAGE_KEY = 'refguard_scan_history_v1';
const API_BASE = window.API_BASE_URL !== undefined ? window.API_BASE_URL : '';
let currentScanId = null;

// Demo Test Scenarios
const presets = {
  upi_refund_trap: {
    type: 'TEXT',
    context: 'SMS',
    value: 'Congratulations! You won ₹5000 cashback scratch card. Enter UPI PIN to claim: upi://pay?pa=fake.refund@okhdfcbank&pn=CashbackRefund&am=5000'
  },
  digital_arrest_scam: {
    type: 'TEXT',
    context: 'SMS',
    value: 'TRAI/CBI Alert: Your Aadhaar is linked to money laundering in 24 bank accounts. Digital arrest warrant issued. Transfer funds to police verification account.'
  },
  legit_cdsl_alert: {
    type: 'TEXT',
    context: 'SMS',
    value: 'CDSL: Debit in A/c **1234 of 50 shares of INFY for settlement on 24-AUG-2026. If not done by you, contact your DP or visit cdslindia.com.'
  },
  trading_tip_scam: {
    type: 'TEXT',
    context: 'Telegram',
    value: 'Open an account with Angel Broking and get 50% guaranteed returns weekly. Deposit ₹10,000 to start trading with us.'
  },
  fake_referral: {
    type: 'URL',
    context: 'WhatsApp',
    value: 'http://free-cashback-loot.xyz/claim?ref=998877'
  },
  legit_merchant: {
    type: 'QR',
    context: 'Unknown',
    value: 'upi://pay?pa=swiggy@icici&pn=SwiggyOrders&am=450&cu=INR'
  }
};

function loadPreset(key) {
  const p = presets[key];
  if (!p) return;

  const typeSelect = document.getElementById('contentTypeSelect');
  const contextSelect = document.getElementById('sourceContextSelect');
  const valueArea = document.getElementById('contentValueArea');

  if (typeSelect) typeSelect.value = p.type;
  if (contextSelect) contextSelect.value = p.context;
  if (valueArea) {
    valueArea.value = p.value;
    valueArea.focus();
  }

  showToast('Loaded scenario preset: ' + key.replace(/_/g, ' '), 'info');
}

// Main Scan Execution
async function executeScan() {
  const typeSelect = document.getElementById('contentTypeSelect');
  const contextSelect = document.getElementById('sourceContextSelect');
  const valueArea = document.getElementById('contentValueArea');

  const contentType = typeSelect ? typeSelect.value : 'TEXT';
  const sourceContext = contextSelect ? contextSelect.value : 'Unknown';
  const contentValue = valueArea ? valueArea.value.trim() : '';

  if (!contentValue) {
    showToast('Please enter or select content to scan.', 'error');
    if (valueArea) valueArea.focus();
    return;
  }

  const desktopEmptyState = document.getElementById('desktopEmptyState');
  const loadingState = document.getElementById('loadingState');
  const errorState = document.getElementById('errorState');
  const resultContent = document.getElementById('resultContent');
  const resultsPanel = document.getElementById('resultsPanel');

  if (desktopEmptyState) desktopEmptyState.style.display = 'none';
  if (resultsPanel) resultsPanel.style.display = 'block';
  if (resultContent) resultContent.style.display = 'none';
  if (errorState) errorState.style.display = 'none';
  if (loadingState) loadingState.style.display = 'block';
  const loadingText = document.getElementById('loadingText');
  if (loadingText) {
    loadingText.innerText = 'Scanning for threats...';
    setTimeout(() => { if (loadingState.style.display === 'block') loadingText.innerText = 'Checking payment intent...'; }, 600);
    setTimeout(() => { if (loadingState.style.display === 'block') loadingText.innerText = 'Analyzing heuristics...'; }, 1200);
  }

  const scanBtn = document.getElementById('scanBtn');
  if (scanBtn) scanBtn.disabled = true;

  const payload = {
    content_type: contentType,
    content_value: contentValue,
    source_context: sourceContext,
    timestamp: new Date().toISOString()
  };

  try {
    const res = await fetch(`${API_BASE}/api/v1/scan`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      throw new Error(errData.error_message || errData.message || `Server returned ${res.status}`);
    }

    const data = await res.json();
    if (loadingState) loadingState.style.display = 'none';
    
    saveToHistory(payload, data);
    renderScanResponse(data);
    showToast('Scan completed successfully', 'success');

  } catch (err) {
    if (loadingState) loadingState.style.display = 'none';
    if (errorState) {
      errorState.style.display = 'block';
      const msg = document.getElementById('errorMessage');
      if (msg) {
        let text = err.message || 'Scan request failed.';
        if (text.includes('Failed to fetch')) text = 'Cannot connect to the protection engine. Please check your internet connection.';
        if (text.includes('JSON')) text = 'Received an invalid response from the protection engine.';
        msg.innerText = text;
      }
    }
    showToast('Scan failed: ' + err.message, 'error');
  } finally {
    if (scanBtn) scanBtn.disabled = false;
  }
}


// ==========================================
// FRONTEND DATA ADAPTER (Workstream P)
// ==========================================
function normalizeScanResponse(rawData) {
  const risk = rawData.risk_assessment || {};
  const decision = rawData.protection_decision || {};
  const mismatch = rawData.payment_intent_mismatch || {};
  const scamChain = rawData.scam_chain || {};
  const evidencePack = rawData.evidence_pack || {};

  return {
    scanId: rawData.scan_id || 'unknown',
    timestamp: rawData.timestamp || new Date().toISOString(),
    
    // Verdict
    severity: risk.risk_severity || 'LOW',
    score: risk.risk_score !== undefined ? risk.risk_score : 0,
    confidence: risk.confidence || 0,
    
    // Explanation
    title: decision.detected_summary || risk.human_explanation || (risk.risk_severity === 'LOW' ? 'No malicious intent detected.' : 'Suspicious activity detected.'),
    explanation: decision.why_it_matters || risk.human_explanation || '',
    action: decision.user_instruction || risk.recommended_action || '',
    
    // Signals
    signals: risk.signals || [],
    
    // Payment Mismatch
    mismatch: {
      detected: mismatch.status === 'DETECTED',
      statedIntent: mismatch.stated_intent || '',
      actualAction: mismatch.actual_payment_action || '',
      direction: mismatch.payment_direction || ''
    },
    
    // Adaptive Intelligence
    adaptiveIntel: {
      archetype: rawData.adaptive_scam_intelligence ? (rawData.adaptive_scam_intelligence.archetype || rawData.adaptive_scam_intelligence.scam_archetype) : null,
      stage: rawData.adaptive_scam_intelligence ? rawData.adaptive_scam_intelligence.current_stage : null,
      objective: (rawData.adaptive_scam_intelligence && rawData.adaptive_scam_intelligence.attacker_objective) 
                 || (mismatch.status === 'DETECTED' ? 'Get you to authorize a payment by making you believe you are receiving a refund or reward.' : risk.human_explanation),
      nextStep: rawData.adaptive_scam_intelligence ? (rawData.adaptive_scam_intelligence.next_likely_step || rawData.adaptive_scam_intelligence.predicted_next_step) : null,
      userRisk: rawData.adaptive_scam_intelligence ? rawData.adaptive_scam_intelligence.user_risk : null
    },
    
    // Scam Chain
    chainNodes: scamChain.nodes || [],
    
    // Evidence
    evidenceItems: evidencePack.items || [],
    
    // Raw (for debugging or history)
    raw: rawData
  };
}

// Render Response View
function renderScanResponse(data) {
  const model = normalizeScanResponse(data);
  currentScanId = model.scanId;
  
  const resultContent = document.getElementById('resultContent');
  if (resultContent) resultContent.style.display = 'block';

  // Verdict Hero
  const banner = document.getElementById('riskBanner');
  const verdictIcon = document.getElementById('verdictIcon');
  const severityTag = document.getElementById('severityTag');
  const decisionTitle = document.getElementById('decisionTitle');
  const scoreVal = document.getElementById('riskScoreVal');

  if (banner) banner.className = 'verdict-hero ' + model.severity.toLowerCase();
  if (scoreVal) scoreVal.innerText = model.score;

  const confVal = document.getElementById('riskConfidenceVal');
  if (confVal) confVal.innerText = Math.round((model.confidence || 0) * 100);
  
  const heroAction = document.getElementById('heroImmediateAction');
  if (heroAction) {
    if (model.action && model.severity !== 'LOW') {
      heroAction.innerText = 'Action: ' + model.action;
      heroAction.style.display = 'block';
    } else {
      heroAction.style.display = 'none';
    }
  }

  
  if (severityTag) {
    if (model.severity === 'CRITICAL') severityTag.innerText = 'CRITICAL RISK';
    else if (model.severity === 'HIGH') severityTag.innerText = 'HIGH RISK';
    else if (model.severity === 'MEDIUM') severityTag.innerText = '⚠ NEEDS CAUTION';
    else severityTag.innerText = '✓ LOOKS SAFE';
  }
  
  if (verdictIcon) {
    if (model.severity === 'CRITICAL') verdictIcon.innerText = '🚨';
    else if (model.severity === 'HIGH') verdictIcon.innerText = '⚠️';
    else if (model.severity === 'MEDIUM') verdictIcon.innerText = '⚠';
    else verdictIcon.innerText = '✓';
  }

  if (decisionTitle) {
    decisionTitle.innerText = model.title;
  }

  // AI Explanation & Signals
  const explanationCard = document.getElementById('explanationCard');
  const detectedSummary = document.getElementById('detectedSummary');
  const whyItMatters = document.getElementById('whyItMatters');
  const dynamicSignalsContainer = document.getElementById('dynamicSignalsContainer');
  
  if (explanationCard) {
    explanationCard.style.display = (model.severity === 'LOW') ? 'none' : 'block';
  }
  
  if (detectedSummary) detectedSummary.innerText = model.explanation;
  if (whyItMatters) whyItMatters.innerText = model.action;

  if (dynamicSignalsContainer) {
    dynamicSignalsContainer.innerHTML = '';
    const friendlySignalMap = {
      'urgency_indicator': { title: 'Urgency', desc: 'The message pressures you to act immediately without thinking.' },
      'authority_impersonation': { title: 'Authority Impersonation', desc: 'The sender claims to represent an official organization.' },
      'sms_authority_impersonation': { title: 'Authority Impersonation', desc: 'The sender claims to represent an official organization.' },
      'digital_arrest_scam': { title: 'Digital Arrest Threat', desc: 'Impersonation of law enforcement combined with pressure to act immediately.' },
      'payment_intent_mismatch': { title: 'Payment Intent Mismatch', desc: 'You were told you would receive money, but the payment request would debit your account.' },
      'deceptive_reward_trigger': { title: 'Deceptive Reward', desc: 'Promises an unexpected reward or refund to trick you into paying.' },
      'hinglish_cashback_scam': { title: 'Cashback Trap', desc: 'Common localized language pattern used in cashback scams.' },
      'financial_reward': { title: 'Fake Reward', desc: 'Promises an unexpected reward or refund to trick you into paying.' },
      'upi_fraud_pattern': { title: 'UPI Collect Fraud', desc: 'Disguises a payment request as a refund or prize receipt.' }
    };

    model.signals.forEach(sig => {
      if (friendlySignalMap[sig]) {
        const div = document.createElement('div');
        div.className = 'signal-item';
        div.innerHTML = `<div class="signal-title">⚠ ${friendlySignalMap[sig].title}</div>
                         <div class="signal-desc">${friendlySignalMap[sig].desc}</div>`;
        dynamicSignalsContainer.appendChild(div);
      }
    });
  }

  const mismatchCard = document.getElementById('mismatchCard');
  if (mismatchCard) {
    if (model.mismatch.detected) {
      mismatchCard.style.display = 'block';
      document.getElementById('statedIntent').innerText = model.mismatch.statedIntent || 'RECEIVE FUNDS';
      document.getElementById('actualPayment').innerText = model.mismatch.actualAction || 'OUTBOUND DEBIT';
      document.getElementById('paymentDirection').innerText = model.mismatch.direction || 'OUTBOUND_DEBIT';
    } else {
      mismatchCard.style.display = 'none';
    }
  }

  // Unified Adaptive Intelligence Card
  const adaptiveIntelCard = document.getElementById('adaptiveIntelCard');
  
  if (adaptiveIntelCard) {
    // Determine visibility based on available fields and severity
    let hasIntel = false;
    
    // Objective / Next Step synthesis
    let nextText = model.adaptiveIntel.nextStep;
    if (!nextText && model.chainNodes.length > 2) {
      const predictedNext = model.chainNodes[2];
      let typeText = predictedNext.node_type ? predictedNext.node_type.replace(/_/g, ' ') : 'ACTION';
      nextText = 'The attacker will likely try to execute a ' + typeText + ' (' + (predictedNext.entity_reference || predictedNext.node_id) + '). Do not proceed.';
    }

    const fields = [
      { id: 'aiScamType', boxId: 'aiScamTypeBox', val: model.adaptiveIntel.archetype },
      { id: 'aiCurrentStage', boxId: 'aiCurrentStageBox', val: model.adaptiveIntel.stage },
      { id: 'aiObjective', boxId: 'aiObjectiveBox', val: model.adaptiveIntel.objective },
      { id: 'aiUserRisk', boxId: 'aiUserRiskBox', val: model.adaptiveIntel.userRisk },
      { id: 'aiNextStep', boxId: 'aiNextStepBox', val: nextText },
      { id: 'aiRecommendedAction', boxId: 'aiRecommendedActionBox', val: model.action } // Reuse the action
    ];

    fields.forEach(f => {
      const el = document.getElementById(f.id);
      const box = document.getElementById(f.boxId);
      if (el && box) {
        if (f.val) {
          el.innerText = f.val;
          box.style.display = 'block';
          hasIntel = true;
        } else {
          box.style.display = 'none';
        }
      }
    });

    if (hasIntel && (model.severity === 'CRITICAL' || model.severity === 'HIGH' || model.severity === 'MEDIUM')) {
      adaptiveIntelCard.style.display = 'block';
    } else {
      adaptiveIntelCard.style.display = 'none';
    }
  }

  // Scam Chain
  const chainCard = document.getElementById('scamChainCard');
  const chainContainer = document.getElementById('adaptiveChainContainer');
  if (chainCard && chainContainer) {
    chainContainer.innerHTML = '';
    if (model.chainNodes.length > 0 && model.severity !== 'LOW') {
      chainCard.style.display = 'block';
      model.chainNodes.forEach((node, idx) => {
        // Consume explicit backend semantics for node status
        const rawStatus = (node.status || node.state || node.node_status || node.observation_status || '').toUpperCase();
        
        let status = 'UNVERIFIED';
        let icon = '⁈';
        let cls = 'inferred';
        
        if (rawStatus === 'OBSERVED') {
          status = 'OBSERVED';
          icon = '✓';
          cls = 'detected';
        } else if (rawStatus === 'INFERRED') {
          status = 'INFERRED';
          icon = '◉';
          cls = 'inferred';
        } else if (rawStatus === 'PREDICTED') {
          status = 'PREDICTED';
          icon = '→';
          cls = 'predicted';
        }

        const div = document.createElement('div');
        div.className = 'chain-node ' + cls;
        
        let confidenceHtml = '';
        if (node.confidence !== undefined) {
           confidenceHtml = `<span style="margin-left: 0.5rem; font-size: 0.8em; opacity: 0.8;">(${Math.round(node.confidence * 100)}% confidence)</span>`;
        }
        
        let provenanceHtml = '';
        if (node.provenance) {
           provenanceHtml = `<div style="font-size: 0.8em; color: var(--text-muted); margin-top: 0.25rem;">Source: ${escapeHtml(node.provenance)}</div>`;
        }
        
        let evidenceHtml = '';
        if (node.evidence_references && node.evidence_references.length > 0) {
           evidenceHtml = `<div style="font-size: 0.8em; color: var(--text-muted); margin-top: 0.1rem;">Evidence Ref: ${escapeHtml(node.evidence_references.join(', '))}</div>`;
        }

        div.innerHTML = `
          <div class="chain-dot"></div>
          <div class="chain-node-title" style="margin-bottom:0.2rem;">${escapeHtml(node.entity_reference || node.node_id)}</div>
          <div class="chain-node-desc">
            <strong>${icon} ${status}</strong> &mdash; ${escapeHtml(node.node_type)}
            ${confidenceHtml}
            ${provenanceHtml}
            ${evidenceHtml}
          </div>
        `;
        chainContainer.appendChild(div);
      });
    } else {
      chainCard.style.display = 'none';
    }
  }

  // Evidence
  const evidenceCard = document.getElementById('evidenceCard');
  const evidenceContainer = document.getElementById('evidenceContainer');
  if (evidenceCard && evidenceContainer) {
    evidenceContainer.innerHTML = '';
    if (model.evidenceItems.length > 0) {
      evidenceCard.style.display = 'block';
      model.evidenceItems.forEach(item => {
        const div = document.createElement('div');
        div.style.marginBottom = '0.5rem';
        div.innerHTML = '<strong>' + escapeHtml(item.evidence_type) + ':</strong> ' + escapeHtml(item.data || '');
        evidenceContainer.appendChild(div);
      });
    } else {
      evidenceCard.style.display = 'none';
    }
  }

  // Trigger Incident
  fetchIncidentRecommendation(data); // Pass raw data here or we can refactor incident later
}
async function fetchIncidentRecommendation(scanResponse) {
  const card = document.getElementById('incidentCard');
  const loading = document.getElementById('incidentLoading');
  const content = document.getElementById('incidentContent');
  const error = document.getElementById('incidentError');
  const safeState = document.getElementById('incidentSafe');
  const retryBtn = document.getElementById('retryIncidentBtn');

  if (!card) return;

  card.style.display = 'block';
  if (loading) loading.style.display = 'flex';
  if (content) content.style.display = 'none';
  if (error) error.style.display = 'none';
  if (safeState) safeState.style.display = 'none';

  if (retryBtn) {
    retryBtn.onclick = () => fetchIncidentRecommendation(scanResponse);
  }

  try {
    const res = await fetch(`${API_BASE}/api/v1/incident/recommendation`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(scanResponse)
    });

    if (!res.ok) throw new Error('Incident API error');

    const data = await res.json();
    const rec = data.incident_recommendation;

    if (loading) loading.style.display = 'none';

    if (!rec || rec.incident_category === 'BENIGN' || scanResponse.risk_assessment?.risk_severity === 'LOW') {
      if (safeState) safeState.style.display = 'block';
      return;
    }

    if (content) content.style.display = 'block';

    const catBadge = document.getElementById('incidentCategoryBadge');
    if (catBadge) catBadge.innerText = (rec.incident_category || 'FRAUD INCIDENT').replace(/_/g, ' ');

    const immEl = document.getElementById('incImmediateAction');
    if (immEl) immEl.innerText = rec.immediate_action || 'Stop all interaction immediately.';

    const payEl = document.getElementById('incPaymentAction');
    if (payEl) payEl.innerText = rec.payment_account_protection_action || 'Do not authorize any transactions.';

    const evEl = document.getElementById('incEvidenceAction');
    if (evEl) evEl.innerText = rec.evidence_preservation_guidance || 'Take screenshots of messages and numbers.';

    const reasonEl = document.getElementById('incReportingReason');
    if (reasonEl) reasonEl.innerText = rec.reporting_reason || 'Severe financial fraud indicators detected.';

    // Safe formatting for clickable reporting destinations
    const destEl = document.getElementById('incReportingDest');
    if (destEl) {
      let rawDest = rec.reporting_destination || 'National Cyber Crime Reporting Portal (cybercrime.gov.in) or call 1930.';
      let formatted = escapeHtml(rawDest);
      // Linkify cybercrime / SEBI domains
      formatted = formatted.replace(/\b(cybercrime\.gov\.in|scores\.gov\.in)\b/gi, '<a href="https://$1" target="_blank" rel="noopener noreferrer" class="tappable-link">$1 ↗</a>');
      // Linkify URLs
      formatted = formatted.replace(/(https?:\/\/[^\s]+)/gi, '<a href="$1" target="_blank" rel="noopener noreferrer" class="tappable-link">$1 ↗</a>');
      // Linkify 1930 Helpline
      formatted = formatted.replace(/\b(1930)\b/g, '<a href="tel:1930" class="tappable-link">📞 $1 (National Cybercrime Helpline)</a>');

      destEl.innerHTML = formatted;
    }

  } catch (err) {
    if (loading) loading.style.display = 'none';
    if (error) error.style.display = 'flex';
  }
}

// Community Scam Report Submission
async function reportScam() {
  const valueArea = document.getElementById('contentValueArea');
  const contentValue = valueArea ? valueArea.value.trim() : '';

  if (!contentValue) {
    showToast('Please enter an indicator or URL to report.', 'error');
    return;
  }

  const reportPayload = {
    report_id: 'rep_' + Math.random().toString(36).substring(2, 10),
    reported_indicator: contentValue.substring(0, 120),
    report_category: 'COMMUNITY_REPORT',
    description: 'Submitted via RefGuard Web Demo Portal: ' + contentValue.substring(0, 80),
    submission_timestamp: new Date().toISOString(),
    moderation_status: 'PENDING',
    confidence: 0.85,
    provenance: 'USER_SUBMISSION'
  };

  try {
    const res = await fetch(`${API_BASE}/api/v1/report`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(reportPayload)
    });

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      throw new Error(errData.error_message || errData.message || 'Report rejected');
    }

    showToast('📢 Threat reported to community registry! Thank you.', 'success');
  } catch (err) {
    showToast('Report submission failed: ' + err.message, 'error');
  }
}

// Block & Protect Action
function triggerBlockProtect() {
  showToast('🛡️ Protective Action: Threat source flagged and blocked in local shield.', 'info');
}

// Feedback Loop (Model Calibration)
async function submitFeedback(verdict) {
  if (!currentScanId) {
    showToast('No active scan to submit feedback for.', 'error');
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/api/v1/feedback`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        scan_id: currentScanId,
        indicator: 'USER_CALIBRATION',
        verdict: verdict,
        user_notes: 'Feedback submitted via web demo UI'
      })
    });

    if (res.ok) {
      const thanks = document.getElementById('feedbackThanks');
      if (thanks) {
        thanks.style.display = 'block';
        setTimeout(() => { thanks.style.display = 'none'; }, 5000);
      }
      showToast('Feedback recorded: ' + verdict.replace(/_/g, ' '), 'success');
    } else {
      const errData = await res.json().catch(() => ({}));
      showToast(errData.message || 'Failed to record feedback', 'error');
    }
  } catch (err) {
    showToast('Error recording feedback: ' + err.message, 'error');
  }
}

// Tab Navigation
function switchTab(tab) {
  const navScanner = document.getElementById('navScanner');
  const navHistory = document.getElementById('navHistory');
  const navIntel = document.getElementById('navIntel');
  const navReport = document.getElementById('navReport');

  const presetsSection = document.getElementById('presetsSection');
  const workspaceGrid = document.getElementById('workspaceGrid');
  const historyView = document.getElementById('historyView');
  const intelView = document.getElementById('intelView');
  const reportView = document.getElementById('reportView');

  if (navScanner) navScanner.classList.remove('active');
  if (navHistory) navHistory.classList.remove('active');
  if (navIntel) navIntel.classList.remove('active');
  if (navReport) navReport.classList.remove('active');

  if (presetsSection) presetsSection.style.display = 'none';
  if (workspaceGrid) workspaceGrid.style.display = 'none';
  if (historyView) historyView.style.display = 'none';
  if (intelView) intelView.style.display = 'none';
  if (reportView) reportView.style.display = 'none';

  if (tab === 'scanner') {
    if (navScanner) navScanner.classList.add('active');
    if (presetsSection) presetsSection.style.display = 'block';
    if (workspaceGrid) workspaceGrid.style.display = 'flex';
  } else if (tab === 'history') {
    if (navHistory) navHistory.classList.add('active');
    if (historyView) historyView.style.display = 'block';
    renderHistory();
  } else if (tab === 'intel') {
    if (navIntel) navIntel.classList.add('active');
    if (intelView) intelView.style.display = 'block';
    loadIntel();
  } else if (tab === 'report') {
    if (navReport) navReport.classList.add('active');
    if (reportView) reportView.style.display = 'block';
  }
}

// Android Hardware Back Button Handling
if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.App) {
  window.Capacitor.Plugins.App.addListener('backButton', () => {
    // Check if we are on a non-scanner tab
    const historyView = document.getElementById('historyView');
    const intelView = document.getElementById('intelView');
    const reportView = document.getElementById('reportView');
    
    if ((historyView && historyView.style.display === 'block') ||
        (intelView && intelView.style.display === 'block') ||
        (reportView && reportView.style.display === 'block')) {
      switchTab('scanner');
      return;
    }

    // Check if we are viewing results on the scanner tab
    const resultsPanel = document.getElementById('resultsPanel');
    const desktopEmptyState = document.getElementById('desktopEmptyState');
    if (resultsPanel && resultsPanel.style.display === 'block') {
      resultsPanel.style.display = 'none';
      if (desktopEmptyState) desktopEmptyState.style.display = 'flex';
      return;
    }

    // Otherwise, exit the app
    window.Capacitor.Plugins.App.exitApp();
  });
}

// Local Scan History
function saveToHistory(payload, response) {
  try {
    let history = [];
    const stored = localStorage.getItem(HISTORY_STORAGE_KEY);
    if (stored) history = JSON.parse(stored);

    let cleanVal = (payload.content_value || '').replace(/\b\d{4,6}\b/g, '[REDACTED PIN/OTP]');
    const historyEntry = {
      id: response.scan_id,
      timestamp: new Date().toISOString(),
      payloadSummary: cleanVal.substring(0, 50) + (cleanVal.length > 50 ? '...' : ''),
      severity: response.risk_assessment?.risk_severity || 'LOW',
      score: response.risk_assessment?.risk_score || 0,
      summary: response.protection_decision?.detected_summary || 'Scan Completed',
      fullResponse: response
    };

    history.unshift(historyEntry);
    if (history.length > 50) history = history.slice(0, 50);

    localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(history));
  } catch (e) {
    console.error('Failed to save scan to localStorage', e);
  }
}

function renderHistory() {
  const container = document.getElementById('historyListContainer');
  if (!container) return;

  container.innerHTML = '';
  let history = [];
  try {
    const stored = localStorage.getItem(HISTORY_STORAGE_KEY);
    if (stored) history = JSON.parse(stored);
  } catch (e) {
    history = [];
  }

  if (history.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">📂</div>
        <h4>No Scan History Yet</h4>
        <p>Run scans in the Scanner tab to build your local privacy-safe threat history.</p>
      </div>
    `;
    return;
  }

  history.forEach(item => {
    const div = document.createElement('div');
    div.className = `history-item ${item.severity}`;
    div.onclick = () => viewHistoryItem(item);

    const dateStr = new Date(item.timestamp).toLocaleString();
    div.innerHTML = `
      <div class="history-content">
        <div class="history-time">${escapeHtml(dateStr)}</div>
        <div class="history-summary">${escapeHtml(item.summary || 'Scan Completed')}</div>
        <div class="history-payload">${escapeHtml(item.payloadSummary || '')}</div>
      </div>
      <div class="history-score ${item.severity}">${item.score}</div>
    `;
    container.appendChild(div);
  });
}

function clearHistory() {
  if (confirm('Are you sure you want to clear your local scan history?')) {
    localStorage.removeItem(HISTORY_STORAGE_KEY);
    renderHistory();
    showToast('Local scan history cleared', 'success');
  }
}

function viewHistoryItem(item) {
  if (!item || !item.fullResponse) return;
  switchTab('scanner');

  const desktopEmptyState = document.getElementById('desktopEmptyState');
  const resultsPanel = document.getElementById('resultsPanel');
  const loadingState = document.getElementById('loadingState');
  const errorState = document.getElementById('errorState');

  if (desktopEmptyState) desktopEmptyState.style.display = 'none';
  if (resultsPanel) resultsPanel.style.display = 'block';
  if (loadingState) loadingState.style.display = 'none';
  if (errorState) errorState.style.display = 'none';

  renderScanResponse(item.fullResponse);
}

// Global Threat Intel Feed
async function loadIntel() {
  const trendingList = document.getElementById('trendingList');
  const reportsList = document.getElementById('reportsList');

  if (trendingList) trendingList.innerHTML = '<div class="intel-card"><p>Loading trending signatures...</p></div>';
  if (reportsList) reportsList.innerHTML = '<div class="intel-card"><p>Loading recent reports...</p></div>';

  try {
    const [trendRes, reportRes] = await Promise.all([
      fetch(`${API_BASE}/api/v1/intel/trending`).catch(() => null),
      fetch(`${API_BASE}/api/v1/intel/reports`).catch(() => null)
    ]);

    if (trendingList) {
      trendingList.innerHTML = '';
      if (trendRes && trendRes.ok) {
        const data = await trendRes.json();
        const items = data.trending_indicators || [];
        if (items.length > 0) {
          items.forEach(ind => {
            const div = document.createElement('div');
            div.className = 'intel-card';
            div.innerHTML = '<h4 style="color: var(--color-critical);">' + escapeHtml(ind.indicator) + '</h4>' +
                             '<p>Severity: <strong>' + escapeHtml(ind.severity || 'CRITICAL') + '</strong> | Sightings: ' + (ind.sightings || ind.reportCount || 1) + '</p>';
            trendingList.appendChild(div);
          });
        } else {
          trendingList.innerHTML = '<div class="intel-card"><p>No trending threats found in registry.</p></div>';
        }
      } else {
        trendingList.innerHTML = '<div class="intel-card"><p>Threat intelligence endpoint unavailable.</p></div>';
      }
    }

    if (reportsList) {
      reportsList.innerHTML = '';
      if (reportRes && reportRes.ok) {
        const data = await reportRes.json();
        const reports = data.recent_reports || data.reports || [];
        if (reports.length > 0) {
          reports.forEach(rep => {
            const div = document.createElement('div');
            div.className = 'intel-card';
            const dateStr = rep.submission_timestamp ? new Date(rep.submission_timestamp).toLocaleString() : 'Recent';
            div.innerHTML = '<h4>' + escapeHtml(rep.reported_indicator || 'Redacted') + '</h4>' +
                             '<p>Category: <strong>' + escapeHtml(rep.report_category || 'UPI_FRAUD') + '</strong> | ' + escapeHtml(dateStr) + '</p>';
            reportsList.appendChild(div);
          });
        } else {
          reportsList.innerHTML = '<div class="intel-card"><p>No community reports filed yet.</p></div>';
        }
      } else {
        reportsList.innerHTML = '<div class="intel-card"><p>Community reports endpoint unavailable.</p></div>';
      }
    }

  } catch (err) {
    if (trendingList) trendingList.innerHTML = '<div class="intel-card"><p style="color: var(--color-critical);">Failed to load trending threats.</p></div>';
    if (reportsList) reportsList.innerHTML = '<div class="intel-card"><p style="color: var(--color-critical);">Failed to load recent reports.</p></div>';
    showToast('Failed to load global intelligence feed', 'error');
  }
}

// Toast System
function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <span>${type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️'}</span>
    <span>${escapeHtml(message)}</span>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// HTML Escaping Utility
function escapeHtml(str) {
  if (typeof str !== 'string') return String(str || '');
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Auto-initialize on load
document.addEventListener('DOMContentLoaded', () => {
  // Check for stored preset or auto-focus
  const valueArea = document.getElementById('contentValueArea');
  if (valueArea && !valueArea.value) {
    // Pre-populate with first preset for instant exploration
    loadPreset('upi_refund_trap');
  }
});

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

    if (res.status === 409) {
      throw new Error('This indicator has already been reported.');
    }
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

function clearScanner() {
  const valueArea = document.getElementById('contentValueArea');
  if (valueArea) {
    valueArea.value = '';
    valueArea.focus();
  }
}

function resetScanner() {
  clearScanner();
  const emptyState = document.getElementById('emptyState');
  const resultContent = document.getElementById('resultContent');
  const errorState = document.getElementById('errorState');
  
  const resultsPanel = document.getElementById('resultsPanel');
  if (resultsPanel) resultsPanel.style.display = 'none';
  const desktopEmptyState = document.getElementById('desktopEmptyState');
  if (desktopEmptyState) desktopEmptyState.style.display = 'none';
  if (resultContent) resultContent.style.display = 'none';
  if (errorState) errorState.style.display = 'none';
  
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
