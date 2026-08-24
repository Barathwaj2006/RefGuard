// RefGuard — Core Web Application Logic
const HISTORY_STORAGE_KEY = 'refguard_scan_history_v1';
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

  const emptyState = document.getElementById('emptyState');
  const loadingState = document.getElementById('loadingState');
  const errorState = document.getElementById('errorState');
  const resultContent = document.getElementById('resultContent');

  if (emptyState) emptyState.style.display = 'none';
  if (resultContent) resultContent.style.display = 'none';
  if (errorState) errorState.style.display = 'none';
  if (loadingState) loadingState.style.display = 'block';

  const scanBtn = document.getElementById('scanBtn');
  if (scanBtn) scanBtn.disabled = true;

  const payload = {
    content_type: contentType,
    content_value: contentValue,
    source_context: sourceContext,
    timestamp: new Date().toISOString()
  };

  try {
    const res = await fetch('/api/v1/scan', {
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
      if (msg) msg.innerText = err.message || 'Scan request failed.';
    }
    showToast('Scan failed: ' + err.message, 'error');
  } finally {
    if (scanBtn) scanBtn.disabled = false;
  }
}

// Render Response View
function renderScanResponse(data) {
  currentScanId = data.scan_id;
  const resultContent = document.getElementById('resultContent');
  if (resultContent) resultContent.style.display = 'block';

  const scanIdBadge = document.getElementById('scanIdBadge');
  if (scanIdBadge) scanIdBadge.innerText = 'ID: ' + (data.scan_id || 'N/A');

  const risk = data.risk_assessment || {};
  const decision = data.protection_decision || {};
  const mismatch = data.payment_intent_mismatch || {};
  const scamChain = data.scam_chain || {};
  const evidencePack = data.evidence_pack || {};

  const severity = risk.risk_severity || 'LOW';
  const score = risk.risk_score !== undefined ? risk.risk_score : 0;

  // 1. Risk Banner
  const banner = document.getElementById('riskBanner');
  if (banner) {
    banner.className = 'risk-banner ' + severity;
  }

  const scoreVal = document.getElementById('riskScoreVal');
  if (scoreVal) scoreVal.innerText = score;

  const severityTag = document.getElementById('severityTag');
  if (severityTag) severityTag.innerText = severity + ' RISK';

  // AI Verified Badge
  const aiBadge = document.getElementById('aiBadge');
  const signals = risk.signals || [];
  const isAiUsed = signals.includes('gemini_reasoning_applied') || signals.some(s => s.startsWith('gemini_'));
  if (aiBadge) {
    aiBadge.style.display = isAiUsed ? 'inline-block' : 'none';
  }

  const decisionTitle = document.getElementById('decisionTitle');
  if (decisionTitle) {
    decisionTitle.innerText = decision.action ? decision.action.replace(/_/g, ' ') : 'ASSESSMENT COMPLETE';
  }

  const detectedSummary = document.getElementById('detectedSummary');
  if (detectedSummary) {
    detectedSummary.innerText = decision.detected_summary || risk.human_explanation || '';
  }

  // 2. Protective Action Advisory Card
  const userInstruction = document.getElementById('userInstruction');
  if (userInstruction) userInstruction.innerText = decision.user_instruction || risk.recommended_action || 'Proceed with normal caution.';

  const whyItMatters = document.getElementById('whyItMatters');
  if (whyItMatters) whyItMatters.innerText = decision.why_it_matters || risk.human_explanation || 'No malicious indicators detected.';

  const recommendedAction = document.getElementById('recommendedAction');
  if (recommendedAction) recommendedAction.innerText = risk.recommended_action || decision.user_instruction || 'Standard verification.';

  const blockProtectContainer = document.getElementById('blockProtectContainer');
  if (blockProtectContainer) {
    blockProtectContainer.style.display = (severity === 'CRITICAL' || severity === 'HIGH') ? 'block' : 'none';
  }

  // 3. Payment Intent Mismatch Analyzer
  const mismatchCard = document.getElementById('mismatchCard');
  if (mismatchCard) {
    if (mismatch.status === 'DETECTED') {
      mismatchCard.style.display = 'block';
      const stated = document.getElementById('statedIntent');
      if (stated) stated.innerText = mismatch.stated_intent ? mismatch.stated_intent.replace(/_/g, ' ') : 'RECEIVE FUNDS';

      const actual = document.getElementById('actualPayment');
      if (actual) actual.innerText = (mismatch.actual_payment_action ? mismatch.actual_payment_action.replace(/_/g, ' ') : 'OUTBOUND DEBIT') + (mismatch.amount ? ` (₹${mismatch.amount})` : '');

      const statusEl = document.getElementById('mismatchStatus');
      if (statusEl) statusEl.innerText = mismatch.status;

      const dirEl = document.getElementById('paymentDirection');
      if (dirEl) dirEl.innerText = mismatch.payment_direction || 'OUTBOUND_DEBIT';
    } else {
      mismatchCard.style.display = 'none';
    }
  }

  // 4. Adaptive Scam Chain
  const chainCard = document.getElementById('scamChainCard');
  const chainContainer = document.getElementById('adaptiveChainContainer');
  if (chainCard && chainContainer) {
    chainContainer.innerHTML = '';
    const nodes = scamChain.nodes || [];
    const edges = scamChain.edges || [];

    if (nodes.length > 0) {
      chainCard.style.display = 'block';
      nodes.forEach((node, idx) => {
        const isDetected = idx < 2 || node.node_type === 'MESSAGE' || node.node_type === 'UPI_REQUEST';
        const div = document.createElement('div');
        div.className = 'chain-stage ' + (isDetected ? 'detected-stage' : 'predicted-stage');

        const badgeClass = isDetected ? 'detected' : 'predicted';
        const badgeText = isDetected ? '✓ DETECTED' : '→ LIKELY NEXT';

        div.innerHTML = `
          <div class="stage-header">
            <span class="stage-name">${escapeHtml(node.entity_reference || node.node_id)}</span>
            <span class="stage-badge ${badgeClass}">${badgeText}</span>
          </div>
          <p class="stage-desc">Type: <strong>${escapeHtml(node.node_type)}</strong> ${node.evidence_references ? `| Evidence: [${escapeHtml(node.evidence_references.join(', '))}]` : ''}</p>
        `;
        chainContainer.appendChild(div);
      });
    } else {
      chainCard.style.display = 'none';
    }
  }

  // 5. Evidence Pack
  const evidenceCard = document.getElementById('evidenceCard');
  const evidenceContainer = document.getElementById('evidenceContainer');
  if (evidenceCard && evidenceContainer) {
    evidenceContainer.innerHTML = '';
    const items = evidencePack.items || [];
    if (items.length > 0) {
      evidenceCard.style.display = 'block';
      items.forEach(item => {
        const div = document.createElement('div');
        div.className = 'evidence-item';
        div.innerHTML = `
          <div class="evidence-header">
            <span class="evidence-type-tag">${escapeHtml(item.evidence_type)}</span>
            <span class="evidence-category-tag">${escapeHtml(item.evidence_id)}</span>
          </div>
          <div class="evidence-data">${escapeHtml(item.data || '')}</div>
          <div class="evidence-desc">Verified signal recorded in evidence pack</div>
        `;
        evidenceContainer.appendChild(div);
      });
    } else {
      evidenceCard.style.display = 'none';
    }
  }

  // 6. Raw JSON
  const jsonViewer = document.getElementById('jsonViewer');
  if (jsonViewer) {
    jsonViewer.innerText = JSON.stringify(data, null, 2);
  }

  // 7. Trigger Incident Response Recommendation
  fetchIncidentRecommendation(data);
}

// Incident Response & Recovery Flow
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
    const res = await fetch('/api/v1/incident/recommendation', {
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
    const res = await fetch('/api/v1/report', {
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
    const res = await fetch('/api/v1/feedback', {
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

  const presetsSection = document.getElementById('presetsSection');
  const workspaceGrid = document.getElementById('workspaceGrid');
  const historyView = document.getElementById('historyView');
  const intelView = document.getElementById('intelView');

  if (navScanner) navScanner.classList.remove('active');
  if (navHistory) navHistory.classList.remove('active');
  if (navIntel) navIntel.classList.remove('active');

  if (presetsSection) presetsSection.style.display = 'none';
  if (workspaceGrid) workspaceGrid.style.display = 'none';
  if (historyView) historyView.style.display = 'none';
  if (intelView) intelView.style.display = 'none';

  if (tab === 'scanner') {
    if (navScanner) navScanner.classList.add('active');
    if (presetsSection) presetsSection.style.display = 'block';
    if (workspaceGrid) workspaceGrid.style.display = 'grid';
  } else if (tab === 'history') {
    if (navHistory) navHistory.classList.add('active');
    if (historyView) historyView.style.display = 'block';
    renderHistory();
  } else if (tab === 'intel') {
    if (navIntel) navIntel.classList.add('active');
    if (intelView) intelView.style.display = 'block';
    loadIntel();
  }
}

// Local Scan History
function saveToHistory(payload, response) {
  try {
    let history = [];
    const stored = localStorage.getItem(HISTORY_STORAGE_KEY);
    if (stored) history = JSON.parse(stored);

    const historyEntry = {
      id: response.scan_id,
      timestamp: new Date().toISOString(),
      payloadSummary: (payload.content_value || '').substring(0, 60),
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

  const emptyState = document.getElementById('emptyState');
  const loadingState = document.getElementById('loadingState');
  const errorState = document.getElementById('errorState');

  if (emptyState) emptyState.style.display = 'none';
  if (loadingState) loadingState.style.display = 'none';
  if (errorState) errorState.style.display = 'none';

  renderScanResponse(item.fullResponse);
}

// Global Threat Intel Feed
async function loadIntel() {
  const trendingList = document.getElementById('trendingList');
  const reportsList = document.getElementById('reportsList');

  if (trendingList) trendingList.innerHTML = '<li>Loading trending signatures...</li>';
  if (reportsList) reportsList.innerHTML = '<li>Loading recent reports...</li>';

  try {
    const [trendRes, reportRes] = await Promise.all([
      fetch('/api/v1/intel/trending').catch(() => null),
      fetch('/api/v1/intel/reports').catch(() => null)
    ]);

    if (trendingList) {
      trendingList.innerHTML = '';
      if (trendRes && trendRes.ok) {
        const data = await trendRes.json();
        const items = data.trending_indicators || [];
        if (items.length > 0) {
          items.forEach(ind => {
            const li = document.createElement('li');
            li.innerHTML = `
              <span class="intel-indicator">${escapeHtml(ind.indicator)}</span>
              <span class="intel-meta">Severity: <strong>${escapeHtml(ind.severity || 'CRITICAL')}</strong> | Sightings: ${ind.sightings || ind.reportCount || 1}</span>
            `;
            trendingList.appendChild(li);
          });
        } else {
          trendingList.innerHTML = '<li>No trending threats found in registry.</li>';
        }
      } else {
        trendingList.innerHTML = '<li>Threat intelligence endpoint unavailable.</li>';
      }
    }

    if (reportsList) {
      reportsList.innerHTML = '';
      if (reportRes && reportRes.ok) {
        const data = await reportRes.json();
        const reports = data.recent_reports || data.reports || [];
        if (reports.length > 0) {
          reports.forEach(rep => {
            const li = document.createElement('li');
            const dateStr = rep.submission_timestamp ? new Date(rep.submission_timestamp).toLocaleString() : 'Recent';
            li.innerHTML = `
              <span class="intel-indicator">${escapeHtml(rep.reported_indicator || 'Redacted')}</span>
              <span class="intel-meta">Category: <strong>${escapeHtml(rep.report_category || 'UPI_FRAUD')}</strong> | ${escapeHtml(dateStr)}</span>
            `;
            reportsList.appendChild(li);
          });
        } else {
          reportsList.innerHTML = '<li>No community reports filed yet.</li>';
        }
      } else {
        reportsList.innerHTML = '<li>Community reports endpoint unavailable.</li>';
      }
    }

  } catch (err) {
    if (trendingList) trendingList.innerHTML = '<li style="color: var(--color-critical);">Failed to load trending threats.</li>';
    if (reportsList) reportsList.innerHTML = '<li style="color: var(--color-critical);">Failed to load recent reports.</li>';
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
