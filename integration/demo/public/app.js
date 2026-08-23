const presets = {
  fake_referral: {
    type: 'URL',
    context: 'com.whatsapp',
    value: 'http://free-cashback-loot.xyz/claim?ref=998877'
  },
  qr_scam: {
    type: 'QR',
    context: 'com.google.android.apps.nbu.paisa.user',
    value: 'upi://pay?pa=scammer@oksbi&pn=RewardClaim&am=2500&cu=INR'
  },
  intent_mismatch: {
    type: 'TEXT',
    context: 'com.phonepe.app',
    value: 'Congratulations! You won Rs. 5000 cashback scratch card. Enter UPI PIN to claim: upi://pay?pa=fake.refund@okhdfcbank&pn=CashbackRefund&am=5000'
  },
  high_risk_vpa: {
    type: 'UPI_VPA',
    context: 'com.refguard.manual',
    value: 'lottery.winner@paytm'
  },
  screenshot_ocr: {
    type: 'IMAGE',
    context: 'com.android.gallery',
    value: 'VGVsZWdyYW0gVGFzayBFYXJuaW5nIFZJUDogRWFybiA1MDAwIGRhaWx5IGJ5IGxpa2luZyB2aWRlb3MuIENvbnRhY3Qgd2EubWUvOTE5ODc2NTQzMjEwIHJlZj1UQVNLOTk='
  },
  legit_merchant: {
    type: 'QR',
    context: 'com.swiggy.consumer',
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
  severityTag.innerText = risk.risk_severity + ' RISK (' + risk.risk_score + '/100)';
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

  // Chain Nodes
  const chainContainer = document.getElementById('chainNodesContainer');
  chainContainer.innerHTML = '';
  if (chain && chain.nodes && chain.nodes.length > 0) {
    chain.nodes.forEach((n, idx) => {
      const pill = document.createElement('div');
      pill.className = 'node-pill';
      pill.innerHTML = '<span class="node-type">' + n.node_type + '</span>: ' + (n.entity_reference || 'Content');
      chainContainer.appendChild(pill);
      if (idx < chain.nodes.length - 1) {
        const arrow = document.createElement('span');
        arrow.innerText = ' ➔ ';
        chainContainer.appendChild(arrow);
      }
    });
  } else {
    chainContainer.innerHTML = '<span style="color: #64748b;">No multi-step chain detected.</span>';
  }

  // Signals List
  const signalsList = document.getElementById('signalsList');
  signalsList.innerHTML = '';
  if (risk.signals && risk.signals.length > 0) {
    risk.signals.forEach(s => {
      const li = document.createElement('li');
      li.innerText = s;
      signalsList.appendChild(li);
    });
  }

  // Raw JSON
  document.getElementById('jsonViewer').innerText = JSON.stringify(data, null, 2);
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
