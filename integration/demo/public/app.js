const presets = {
  fake_referral: {
    type: 'URL',
    context: 'WhatsApp',
    value: 'http://free-cashback-loot.xyz/claim?ref=998877'
  },
  qr_scam: {
    type: 'QR',
    context: 'Unknown',
    value: 'upi://pay?pa=scammer@oksbi&pn=RewardClaim&am=2500&cu=INR'
  },
  intent_mismatch: {
    type: 'TEXT',
    context: 'SMS',
    value: 'Congratulations! You won Rs. 5000 cashback scratch card. Enter UPI PIN to claim: upi://pay?pa=fake.refund@okhdfcbank&pn=CashbackRefund&am=5000'
  },
  high_risk_vpa: {
    type: 'UPI_VPA',
    context: 'Web',
    value: 'lottery.winner@paytm'
  },
  screenshot_ocr: {
    type: 'IMAGE',
    context: 'Telegram',
    value: 'VGVsZWdyYW0gVGFzayBFYXJuaW5nIFZJUDogRWFybiA1MDAwIGRhaWx5IGJ5IGxpa2luZyB2aWRlb3MuIENvbnRhY3Qgd2EubWUvOTE5ODc2NTQzMjEwIHJlZj1UQVNLOTk='
  },
  legit_merchant: {
    type: 'UPI_VPA',
    context: 'Unknown',
    value: 'upi://pay?pa=swiggy@icici&pn=SwiggyOrders&am=450&cu=INR'
  }
};

function loadPreset(key) {
  const p = presets[key];
  if (!p) return;
  document.getElementById('contentTypeSelect').value = p.type;
  document.getElementById('sourceContextInput').value = p.context;
  document.getElementById('contentValueArea').value = p.value;
  updateChannelLabel();
  executeScan();
}

function updateChannelLabel() {
  const val = document.getElementById('contentTypeSelect').value;
  document.getElementById('channelLabel').innerText = val;
}

async function executeScan() {
  const contentType = document.getElementById('contentTypeSelect').value;
  const sourceContext = document.getElementById('sourceContextInput').value;
  const contentValue = document.getElementById('contentValueArea').value;

  if (!contentValue.trim()) {
    showToast('Please enter content to scan.', 'error');
    return;
  }

  const payload = {
    content_type: contentType,
    content_value: contentValue.trim(),
    source_context: sourceContext.trim() || undefined,
    timestamp: new Date().toISOString()
  };

  document.getElementById('emptyState').style.display = 'none';
  document.getElementById('loadingState').style.display = 'block';
  document.getElementById('resultContent').style.display = 'none';

  document.getElementById('errorState').style.display = 'none';

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 12000); // 12 second timeout

    const res = await fetch('/api/v1/scan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      signal: controller.signal
    });
    clearTimeout(timeoutId);

    const data = await res.json();
    document.getElementById('loadingState').style.display = 'none';

    if (!res.ok) {
      document.getElementById('errorState').style.display = 'block';
      document.getElementById('errorMessage').innerText = data.error_message || data.message || 'Scan failed to process (API Error)';
      return;
    }

    if (!data || !data.risk_assessment || !data.protection_decision) {
      throw new Error('Malformed response received from backend.');
    }

    saveToHistory(payload, data);
    renderScanResponse(data);
  } catch (err) {
    document.getElementById('loadingState').style.display = 'none';
    document.getElementById('errorState').style.display = 'block';
    if (err.name === 'AbortError') {
      document.getElementById('errorMessage').innerText = 'Request Timed Out: The analysis took too long.';
    } else {
      document.getElementById('errorMessage').innerText = 'Network Error: Failed to connect to RefGuard backend. ' + err.message;
    }
  }
}

function renderScanResponse(data) {
  document.getElementById('resultContent').style.display = 'block';
  document.getElementById('scanIdBadge').innerText = data.scan_id;

  const risk = data.risk_assessment;
  const decision = data.protection_decision;
  const mismatch = data.payment_intent_mismatch;
  const chain = data.scam_chain;

  // Banner & Score
  const banner = document.getElementById('riskBanner');
  banner.className = 'risk-banner ' + risk.risk_severity;
  
  const scoreVal = document.getElementById('riskScoreVal');
  scoreVal.innerText = risk.risk_score;
  scoreVal.className = 'risk-score-circle ' + risk.risk_severity;

  const severityTag = document.getElementById('severityTag');
  const severityMap = {
    'CRITICAL': 'CRITICAL RISK',
    'HIGH': 'HIGH RISK',
    'MEDIUM': 'CAUTION',
    'LOW': 'SAFE'
  };
  severityTag.innerText = severityMap[risk.risk_severity] + ' (' + risk.risk_score + '/100)';
  severityTag.className = 'severity-tag ' + risk.risk_severity;

  const aiBadge = document.getElementById('aiBadge');
  if (risk.signals && risk.signals.includes('gemini_reasoning_applied')) {
    aiBadge.style.display = 'inline-block';
  } else {
    aiBadge.style.display = 'none';
  }

  const decisionTitle = document.getElementById('decisionTitle');
  decisionTitle.innerText = decision.action.replace(/_/g, ' ');
  decisionTitle.className = 'decision-title ' + risk.risk_severity;

  document.getElementById('detectedSummary').innerText = decision.detected_summary;

  // Advisory
  const userInstruction = document.getElementById('userInstruction');
  userInstruction.innerText = decision.user_instruction;
  userInstruction.className = 'highlight-instruction ' + risk.risk_severity;

  document.getElementById('whyItMatters').innerText = decision.why_it_matters;
  document.getElementById('recommendedAction').innerText = risk.recommended_action;

  const blockProtectContainer = document.getElementById('blockProtectContainer');
  if (risk.risk_severity === 'CRITICAL' || risk.risk_severity === 'HIGH') {
    blockProtectContainer.style.display = 'block';
  } else {
    blockProtectContainer.style.display = 'none';
  }

  // Mismatch
  if (mismatch && mismatch.status === 'DETECTED') {
    document.getElementById('mismatchCard').style.display = 'block';
    document.getElementById('statedIntent').innerText = mismatch.stated_intent || 'Receive funds';
    document.getElementById('actualPayment').innerText = mismatch.actual_payment_action || 'Outbound debit';
    document.getElementById('mismatchStatus').innerText = mismatch.status;
    document.getElementById('paymentDirection').innerText = mismatch.payment_direction;
  } else {
    document.getElementById('mismatchCard').style.display = 'none';
  }

  // Adaptive Scam Chain
  const adaptiveChain = data.adaptive_scam_chain;
  const adaptiveContainer = document.getElementById('adaptiveChainContainer');
  const fallbackChain = document.getElementById('chainNodesContainer');
  document.getElementById('scamChainCard').style.display = 'block';

  if (adaptiveChain) {
    fallbackChain.style.display = 'none';
    adaptiveContainer.style.display = 'flex';
    adaptiveContainer.className = 'adaptive-chain';
    
    // Distinguish DETECTED vs LIKELY
    const stateClass = `state-${adaptiveChain.state || 'ACTIVE_NOW'}`;
    const detectedTitle = adaptiveChain.state === 'ANTICIPATED' ? 'Likely Future Stage' : 'Detected Stage';
    const badge = adaptiveChain.state === 'ANTICIPATED' ? '<span class="likely-badge">→ LIKELY NEXT</span>' : '<span class="detected-badge">✓ DETECTED</span>';

    adaptiveContainer.innerHTML = `
      <div class="chain-stage ${stateClass}">
        <div class="stage-meta">
          <span>Stage ${adaptiveChain.stageIndex || '?'} of ${adaptiveChain.totalStages || '?'}</span>
          <span>${detectedTitle}</span>
        </div>
        <h5 class="stage-title">${badge}${adaptiveChain.stageTitle || adaptiveChain.currentStage || 'Unknown Stage'}</h5>
        <div class="stage-details">
          <div><strong>Evidence:</strong> ${adaptiveChain.evidenceDetected ? adaptiveChain.evidenceDetected.join(', ') : 'None'}</div>
          <div><strong>Objective:</strong> ${adaptiveChain.attackerObjective || 'Unknown'}</div>
          <div><strong>Previous:</strong> ${adaptiveChain.previousLikelyStage || 'None'}</div>
          <div><strong>Next Step:</strong> ${adaptiveChain.nextLikelyStep || 'None'}</div>
          <div><strong>Risk:</strong> ${adaptiveChain.userRisk || 'Unknown'}</div>
          <div><strong>Action:</strong> ${adaptiveChain.recommendedAction || 'None'}</div>
          <div><strong>Confidence:</strong> ${adaptiveChain.confidence ? (adaptiveChain.confidence * 100).toFixed(0) + '%' : 'N/A'}</div>
        </div>
        ${adaptiveChain.reportingPath ? `
          <div class="reporting-action">
            <a href="${adaptiveChain.reportingPath}" target="_blank" class="reporting-btn">Report to Authority</a>
          </div>
        ` : ''}
      </div>
    `;
  } else {
    adaptiveContainer.style.display = 'none';
    fallbackChain.style.display = 'flex';
    fallbackChain.innerHTML = '';
    if (chain && chain.nodes && chain.nodes.length > 0) {
      chain.nodes.forEach((n, idx) => {
        const pill = document.createElement('div');
        pill.className = 'node-pill';
        pill.innerHTML = '<span class="node-type">' + n.node_type + '</span>: ' + (n.entity_reference || 'Content');
        fallbackChain.appendChild(pill);
        if (idx < chain.nodes.length - 1) {
          const arrow = document.createElement('span');
          arrow.style.color = 'var(--text-muted)';
          arrow.style.fontSize = '1.2rem';
          arrow.innerHTML = '&rarr;';
          fallbackChain.appendChild(arrow);
        }
      });
    } else {
      fallbackChain.innerHTML = '<span style="color: #64748b;">No multi-step chain detected.</span>';
    }
  }

  // Evidence Pack
  const evidencePack = data.evidence_pack;
  const evidenceContainer = document.getElementById('evidenceContainer');
  const fallbackSignals = document.getElementById('signalsList');
  document.getElementById('evidenceCard').style.display = 'block';

  if (evidencePack && evidencePack.items && evidencePack.items.length > 0) {
    fallbackSignals.style.display = 'none';
    evidenceContainer.style.display = 'grid';
    evidenceContainer.innerHTML = '';
    
    evidencePack.items.forEach(item => {
      const div = document.createElement('div');
      div.className = 'evidence-item';
      div.innerHTML = `
        <div class="evidence-header">
          <span class="evidence-type">${item.evidence_type}</span>
        </div>
        <div class="evidence-data">${item.data}</div>
        <div class="evidence-desc">${item.evidence_id}</div>
      `;
      evidenceContainer.appendChild(div);
    });
  } else {
    evidenceContainer.style.display = 'none';
    fallbackSignals.style.display = 'block';
    fallbackSignals.innerHTML = '';
    if (risk.signals && risk.signals.length > 0) {
      risk.signals.forEach(s => {
        const li = document.createElement('li');
        li.innerText = s;
        fallbackSignals.appendChild(li);
      });
    } else {
      fallbackSignals.innerHTML = '<span style="color: #64748b;">No explicit threat signals detected.</span>';
    }
  }

  // Raw JSON
  document.getElementById('jsonViewer').innerText = JSON.stringify(data, null, 2);

  // Trigger Incident Recommendation
  fetchIncidentRecommendation(data);
}

async function reportScam() {
  const contentValue = document.getElementById('contentValueArea').value;
  if (!contentValue.trim()) {
    showToast('Please enter content to report.', 'error');
    return;
  }

  const reportPayload = {
    report_id: 'rep_' + Math.random().toString(36).substring(2, 10),
    timestamp: new Date().toISOString(),
    reporter_type: 'USER',
    content_value: contentValue.trim(),
    scam_category: 'FAKE_REFERRAL',
    description: 'Reported via RefGuard Web Demo Portal'
  };

  try {
    const res = await fetch('/api/v1/report', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(reportPayload)
    });
    const data = await res.json();
    if (res.ok) {
      showToast('Scam successfully reported to community registry! (Report ID: ' + data.report_id + ')', 'success');
    } else {
      showToast('Report failed: ' + (data.message || data.error_message), 'error');
    }
  } catch (err) {
    showToast('Error: ' + err.message, 'error');
  }
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = 'toast ' + type;
  
  let icon = 'ℹ️';
  if (type === 'success') icon = '✅';
  if (type === 'error') icon = '❌';
  
  toast.innerHTML = '<span>' + icon + '</span><span>' + message + '</span>';
  
  container.appendChild(toast);
  
  setTimeout(() => {
    toast.style.animation = 'fadeOut 0.3s ease-out forwards';
    setTimeout(() => {
      if (container.contains(toast)) {
        container.removeChild(toast);
      }
    }, 300);
  }, 4000);
}


// --- History Module ---
const HISTORY_STORAGE_KEY = "refguard_scan_history";

function switchTab(tab) {
  const scannerNav = document.getElementById("navScanner");
  const historyNav = document.getElementById("navHistory");
  const presetsSection = document.getElementById("presetsSection");
  const workspaceGrid = document.getElementById("workspaceGrid");
  const historyView = document.getElementById("historyView");

  if (tab === "scanner") {
    scannerNav.classList.add("active");
    historyNav.classList.remove("active");
    presetsSection.style.display = "block";
    workspaceGrid.style.display = "grid";
    historyView.style.display = "none";
  } else if (tab === "history") {
    historyNav.classList.add("active");
    scannerNav.classList.remove("active");
    presetsSection.style.display = "none";
    workspaceGrid.style.display = "none";
    historyView.style.display = "block";
    renderHistory();
  }
}

function saveToHistory(payload, response) {
  try {
    let history = [];
    try {
      const stored = localStorage.getItem(HISTORY_STORAGE_KEY);
      if (stored) {
        history = JSON.parse(stored);
      }
    } catch (e) {
      console.warn("Corrupted history data. Resetting.", e);
      history = [];
    }

    const historyEntry = {
      id: response.scan_id,
      timestamp: new Date().toISOString(),
      payloadSummary: (payload.content_value || "").substring(0, 60),
      severity: response.risk_assessment.risk_severity,
      score: response.risk_assessment.risk_score,
      summary: response.protection_decision.detected_summary,
      fullResponse: response // Storing the full response so we can view it later
    };

    history.unshift(historyEntry); // Add to beginning
    if (history.length > 50) history = history.slice(0, 50); // Keep last 50 items

    localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(history));
  } catch (e) {
    console.error("Failed to save history", e);
  }
}

function renderHistory() {
  const container = document.getElementById("historyListContainer");
  container.innerHTML = "";
  
  let history = [];
  try {
    const stored = localStorage.getItem(HISTORY_STORAGE_KEY);
    if (stored) {
      history = JSON.parse(stored);
    }
  } catch (e) {
    history = [];
  }

  if (history.length === 0) {
    container.innerHTML = `<div class="empty-state">
      <div class="empty-icon">📂</div>
      <h4>No History Found</h4>
      <p>Your recent scans will appear here.</p>
    </div>`;
    return;
  }

  history.forEach(item => {
    const div = document.createElement("div");
    div.className = `history-item ${item.severity}`;
    div.onclick = () => viewHistoryItem(item);
    
    const dateStr = new Date(item.timestamp).toLocaleString();
    
    div.innerHTML = `
      <div class="history-content">
        <div class="history-time">${dateStr}</div>
        <div class="history-summary">${item.summary || "Scan Completed"}</div>
        <div class="history-payload">${item.payloadSummary || ""}</div>
      </div>
      <div class="history-score ${item.severity}">${item.score}</div>
    `;
    container.appendChild(div);
  });
}

function clearHistory() {
  if (confirm("Are you sure you want to clear your local scan history?")) {
    localStorage.removeItem(HISTORY_STORAGE_KEY);
    renderHistory();
    showToast("History cleared.", "success");
  }
}

function viewHistoryItem(item) {
  if (!item || !item.fullResponse) return;
  // Load into scanner
  switchTab("scanner");
  
  // Hide empty/loading/error
  document.getElementById("emptyState").style.display = "none";
  document.getElementById("loadingState").style.display = "none";
  document.getElementById("errorState").style.display = "none";
  
  // Re-render
  renderScanResponse(item.fullResponse);
}


// --- Incident Response Module ---
async function fetchIncidentRecommendation(scanResponse) {
  const card = document.getElementById("incidentCard");
  const loading = document.getElementById("incidentLoading");
  const content = document.getElementById("incidentContent");
  const error = document.getElementById("incidentError");
  const safeState = document.getElementById("incidentSafe");
  const retryBtn = document.getElementById("retryIncidentBtn");
  
  card.style.display = "block";
  loading.style.display = "flex";
  content.style.display = "none";
  error.style.display = "none";
  safeState.style.display = "none";

  retryBtn.onclick = () => fetchIncidentRecommendation(scanResponse);

  // Do not request for SAFE/LOW severity unless explicitly needed, we can short-circuit if preferred, 
  // but the endpoint handles LOW and returns BENIGN. Lets call the endpoint to be safe and use actual backend semantics.
  try {
    const res = await fetch("/api/v1/incident/recommendation", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(scanResponse)
    });
    
    if (!res.ok) throw new Error("Incident API error");
    
    const data = await res.json();
    const rec = data.incident_recommendation;
    
    loading.style.display = "none";

    if (!rec || rec.incident_category === "BENIGN") {
      safeState.style.display = "block";
      return;
    }

    content.style.display = "block";
    document.getElementById("incidentCategoryBadge").innerText = rec.incident_category.replace(/_/g, " ");
    
    document.getElementById("incImmediateAction").innerText = rec.immediate_action;
    document.getElementById("incPaymentAction").innerText = rec.payment_account_protection_action;
    document.getElementById("incEvidenceAction").innerText = rec.evidence_preservation_guidance;
    
    document.getElementById("incReportingReason").innerText = rec.reporting_reason;

    // Formatting Reporting Dest to make URLs/Phone numbers tappable
    let reportingHtml = rec.reporting_destination;
    // Catch common domain patterns without http
    reportingHtml = reportingHtml.replace(/\b(cybercrime\.gov\.in|scores\.gov\.in)\b/gi, `<a href="https://$1" target="_blank" class="tappable-link">$1</a>`);
    // Basic regex for URLs with http
    reportingHtml = reportingHtml.replace(/(https?:\/\/[^\s]+)/g, `<a href="$1" target="_blank" class="tappable-link">$1</a>`);
    // Basic regex for 1930
    reportingHtml = reportingHtml.replace(/\b(1930)\b/g, `<a href="tel:$1" class="tappable-link">$1</a>`);
    
    document.getElementById("incReportingDest").innerHTML = reportingHtml;

  } catch (err) {
    console.error(err);
    loading.style.display = "none";
    error.style.display = "flex";
  }
}

