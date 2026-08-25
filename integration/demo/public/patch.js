const fs = require('fs');
let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

app = app.replace(
  /if \(loadingState\) loadingState\.style\.display = 'block';/,
  `if (loadingState) loadingState.style.display = 'block';
  const loadingText = document.getElementById('loadingText');
  if (loadingText) {
    loadingText.innerText = 'Scanning for threats...';
    setTimeout(() => { if (loadingState.style.display === 'block') loadingText.innerText = 'Checking payment intent...'; }, 600);
    setTimeout(() => { if (loadingState.style.display === 'block') loadingText.innerText = 'Analyzing heuristics...'; }, 1200);
  }`
);

const renderStart = app.indexOf('function renderScanResponse(data)');
const incidentStart = app.indexOf('async function fetchIncidentRecommendation');

if (renderStart !== -1 && incidentStart !== -1) {
  const newRender = `function renderScanResponse(data) {
  currentScanId = data.scan_id;
  const resultContent = document.getElementById('resultContent');
  if (resultContent) resultContent.style.display = 'block';

  const risk = data.risk_assessment || {};
  const decision = data.protection_decision || {};
  const mismatch = data.payment_intent_mismatch || {};
  const scamChain = data.scam_chain || {};
  const evidencePack = data.evidence_pack || {};

  const severity = risk.risk_severity || 'LOW';
  const score = risk.risk_score !== undefined ? risk.risk_score : 0;

  // Verdict Hero
  const banner = document.getElementById('riskBanner');
  const verdictIcon = document.getElementById('verdictIcon');
  const severityTag = document.getElementById('severityTag');
  const decisionTitle = document.getElementById('decisionTitle');
  const scoreVal = document.getElementById('riskScoreVal');

  if (banner) banner.className = 'verdict-hero ' + severity.toLowerCase();
  if (scoreVal) scoreVal.innerText = score;
  
  if (severityTag) {
    if (severity === 'CRITICAL') severityTag.innerText = 'CRITICAL SCAM RISK';
    else if (severity === 'HIGH') severityTag.innerText = 'HIGH RISK';
    else if (severity === 'MEDIUM') severityTag.innerText = 'BE CAREFUL';
    else severityTag.innerText = 'LIKELY SAFE';
  }
  
  if (verdictIcon) {
    if (severity === 'CRITICAL') verdictIcon.innerText = '🚨';
    else if (severity === 'HIGH') verdictIcon.innerText = '⚠️';
    else if (severity === 'MEDIUM') verdictIcon.innerText = '👀';
    else verdictIcon.innerText = '✅';
  }

  if (decisionTitle) {
    decisionTitle.innerText = decision.detected_summary || risk.human_explanation || (severity === 'LOW' ? 'No malicious intent detected.' : 'Suspicious activity detected.');
  }

  // AI Explanation & Mismatch
  const explanationCard = document.getElementById('explanationCard');
  const detectedSummary = document.getElementById('detectedSummary');
  const whyItMatters = document.getElementById('whyItMatters');
  
  if (explanationCard) {
    explanationCard.style.display = (severity === 'LOW') ? 'none' : 'block';
  }
  if (detectedSummary) detectedSummary.innerText = decision.why_it_matters || risk.human_explanation || '';
  if (whyItMatters) whyItMatters.innerText = decision.user_instruction || risk.recommended_action || '';

  const mismatchCard = document.getElementById('mismatchCard');
  if (mismatchCard) {
    if (mismatch.status === 'DETECTED') {
      mismatchCard.style.display = 'block';
      document.getElementById('statedIntent').innerText = mismatch.stated_intent || 'RECEIVE FUNDS';
      document.getElementById('actualPayment').innerText = mismatch.actual_payment_action || 'OUTBOUND DEBIT';
      document.getElementById('paymentDirection').innerText = mismatch.payment_direction || 'OUTBOUND_DEBIT';
    } else {
      mismatchCard.style.display = 'none';
    }
  }

  // Scam Chain
  const chainCard = document.getElementById('scamChainCard');
  const chainContainer = document.getElementById('adaptiveChainContainer');
  if (chainCard && chainContainer) {
    chainContainer.innerHTML = '';
    const nodes = scamChain.nodes || [];
    if (nodes.length > 0) {
      chainCard.style.display = 'block';
      nodes.forEach((node, idx) => {
        const isDetected = idx < 2 || node.node_type === 'MESSAGE' || node.node_type === 'UPI_REQUEST';
        const div = document.createElement('div');
        div.className = 'chain-node ' + (isDetected ? 'detected' : 'predicted');
        div.innerHTML = '<div class="chain-dot"></div>' +
          '<div class="chain-node-title">' + escapeHtml(node.entity_reference || node.node_id) + '</div>' +
          '<div class="chain-node-desc">' + escapeHtml(node.node_type) + (isDetected ? ' ✓ Detected' : ' → Likely next') + '</div>';
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
    const items = evidencePack.items || [];
    if (items.length > 0) {
      evidenceCard.style.display = 'block';
      items.forEach(item => {
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
  fetchIncidentRecommendation(data);
}

`;
  app = app.substring(0, renderStart) + newRender + app.substring(incidentStart);
}

app = app.replace(
  /if \(content\) content\.style\.display = 'block';[\s\S]*?\/\/ Safe path/m,
  `if (content) content.style.display = 'block';

      document.getElementById('incidentCategoryBadge').innerText = incidentData.incident_category || 'FRAUD PREVENTION';

      const v1 = document.getElementById('incImmediateAction');
      const v2 = document.getElementById('incPaymentAction');
      const v3 = document.getElementById('incEvidenceAction');
      const v4 = document.getElementById('incReportingDest');

      if (v1 && incidentData.immediate_action) {
        v1.innerText = incidentData.immediate_action;
        document.getElementById('incImmediateActionContainer').style.display = 'flex';
      } else {
        document.getElementById('incImmediateActionContainer').style.display = 'none';
      }

      if (v2 && incidentData.payment_protection_action) {
        v2.innerText = incidentData.payment_protection_action;
        document.getElementById('incPaymentActionContainer').style.display = 'flex';
      } else {
        document.getElementById('incPaymentActionContainer').style.display = 'none';
      }

      if (v3 && incidentData.evidence_preservation_action) {
        v3.innerText = incidentData.evidence_preservation_action;
        document.getElementById('incEvidenceActionContainer').style.display = 'flex';
      } else {
        document.getElementById('incEvidenceActionContainer').style.display = 'none';
      }
      
      if (v4 && incidentData.reporting_destination) {
        v4.innerHTML = escapeHtml(incidentData.reporting_destination)
          .replace(/(https?:\\/\\/[^\\s]+)/g, '<a href="$1" target="_blank">$1</a>')
          .replace(/(1930)/g, '<strong>📞 1930</strong>');
        document.getElementById('incReportingDestContainer').style.display = 'flex';
      } else {
        document.getElementById('incReportingDestContainer').style.display = 'none';
      }

    } else {
      // Safe path`
);

fs.writeFileSync('integration/demo/public/app.js', app);
