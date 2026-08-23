// RefGuard Interactive Demo — Client-Side Application

const scenarios = {
  'fake-referral': {
    type: 'URL',
    value: 'https://free-cashback.tk/referral?code=abc123&utm_source=whatsapp'
  },
  'tampered-qr': {
    type: 'QR',
    value: 'upi://pay?pa=fraudster.collect@okhdfcbank&pn=Amazon%20Rewards&tn=Claim%20Prize&am=500&tr=12345'
  },
  'payment-mismatch': {
    type: 'TEXT',
    value: 'You have won a prize of ₹10,000! Click here to claim. Send payment via UPI to verify account.'
  },
  'high-risk-vpa': {
    type: 'UPI_VPA',
    value: 'secure.payment.verify@hdbank'
  },
  'legitimate-merchant': {
    type: 'UPI_VPA',
    value: 'amazon.payments@okhdfcbank'
  },
  'advance-fee': {
    type: 'TEXT',
    value: 'Claim your lottery winnings! You have won $50,000 in the international lottery. Pay ₹5,000 processing fee now to receive payment.'
  }
};

class RefGuardDemo {
  constructor() {
    this.apiUrl = 'http://localhost:3000/api/v1/scan';
    this.initElements();
    this.attachEventListeners();
  }

  initElements() {
    this.contentTypeSelect = document.getElementById('content-type');
    this.contentValueField = document.getElementById('content-value');
    this.scanButton = document.getElementById('scan-button');
    this.scanLoading = document.getElementById('scan-loading');
    this.noResponseState = document.getElementById('no-response');
    this.responseContainer = document.getElementById('response-container');
    this.scenarioButtons = document.querySelectorAll('.scenario-btn');
    this.toggleJsonBtn = document.getElementById('toggle-json');
    this.rawJsonPre = document.getElementById('raw-json');
  }

  attachEventListeners() {
    this.scanButton.addEventListener('click', () => this.performScan());
    this.toggleJsonBtn.addEventListener('click', () => this.toggleRawJson());
    this.scenarioButtons.forEach(btn => {
      btn.addEventListener('click', (e) => this.loadScenario(e.target.closest('.scenario-btn').dataset.scenario));
    });
  }

  loadScenario(scenarioKey) {
    const scenario = scenarios[scenarioKey];
    if (scenario) {
      this.contentTypeSelect.value = scenario.type;
      this.contentValueField.value = scenario.value;
      this.contentValueField.focus();
      // Scroll to the scan button
      this.scanButton.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }

  async performScan() {
    const contentType = this.contentTypeSelect.value;
    const contentValue = this.contentValueField.value.trim();

    if (!contentValue) {
      alert('Please enter or select content to scan.');
      return;
    }

    const scanRequest = {
      content_type: contentType,
      content_value: contentValue,
      timestamp: new Date().toISOString(),
      source_context: 'refguard.demo.web'
    };

    this.scanButton.disabled = true;
    this.scanLoading.style.display = 'flex';
    this.noResponseState.style.display = 'none';
    this.responseContainer.style.display = 'none';

    try {
      const response = await fetch(this.apiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(scanRequest)
      });

      const data = await response.json();

      if (response.ok) {
        this.displayResponse(data);
      } else {
        this.displayError(data);
      }
    } catch (error) {
      this.displayNetworkError(error.message);
    } finally {
      this.scanButton.disabled = false;
      this.scanLoading.style.display = 'none';
    }
  }

  displayResponse(data) {
    this.noResponseState.style.display = 'none';
    this.responseContainer.style.display = 'block';

    // Display raw JSON
    this.rawJsonPre.textContent = JSON.stringify(data, null, 2);
    document.getElementById('raw-json').style.display = 'none';
    this.toggleJsonBtn.textContent = 'Show';

    // Risk Assessment
    const riskAssessment = data.risk_assessment || {};
    const riskScore = Math.round(riskAssessment.score || 0);
    const riskSeverity = (riskAssessment.severity || 'UNKNOWN').toLowerCase();

    document.getElementById('risk-score').textContent = riskScore;
    document.getElementById('risk-level').textContent = `Risk Level: ${riskAssessment.severity || 'UNKNOWN'}`;
    document.getElementById('risk-reasoning').textContent = riskAssessment.reasoning || 'No reasoning provided.';

    // Update risk circle styling
    const riskCircle = document.querySelector('.risk-score-circle');
    riskCircle.className = 'risk-score-circle';
    if (riskScore < 30) {
      riskCircle.classList.add('low');
    } else if (riskScore < 70) {
      riskCircle.classList.add('medium');
    } else {
      riskCircle.classList.add('high');
    }

    // Protection Decision
    const decision = data.protection_decision || {};
    const decisionAction = (decision.action || 'UNKNOWN').replace(/_/g, ' ');
    const decisionContent = document.getElementById('protection-decision-content');

    decisionContent.innerHTML = `
      <p class="decision-action ${decision.action?.toLowerCase().replace(/_/g, '-')}">${decisionAction}</p>
      <p class="decision-reason">${decision.reason || 'No reason provided.'}</p>
      <p class="decision-guidance">${decision.user_guidance || 'No guidance available.'}</p>
    `;

    // Payment Intent Mismatch
    const mismatchCard = document.getElementById('mismatch-card');
    if (data.payment_intent_mismatch && Object.keys(data.payment_intent_mismatch).length > 0) {
      const mismatch = data.payment_intent_mismatch;
      mismatchCard.style.display = 'block';
      document.getElementById('mismatch-content').innerHTML = `
        <p><strong>Stated Intent:</strong> ${mismatch.stated_intent || 'N/A'}</p>
        <p><strong>Actual Action:</strong> ${mismatch.actual_action || 'N/A'}</p>
        <p><strong>Mismatch Confidence:</strong> ${mismatch.confidence_score || 'N/A'}</p>
        <p><strong>Details:</strong> ${mismatch.details || 'No details.'}</p>
      `;
    } else {
      mismatchCard.style.display = 'none';
    }

    // Scam Chain
    const scamchainCard = document.getElementById('scamchain-card');
    if (data.scam_chain && Object.keys(data.scam_chain).length > 0) {
      const chain = data.scam_chain;
      scamchainCard.style.display = 'block';
      const nodes = chain.chain_nodes || [];
      const edges = chain.chain_edges || [];
      document.getElementById('scamchain-content').innerHTML = `
        <p><strong>Nodes:</strong> ${nodes.length > 0 ? nodes.map(n => n.label || 'Unknown').join(' → ') : 'None'}</p>
        <p><strong>Edges:</strong> ${edges.length > 0 ? edges.map(e => `${e.source} → ${e.target}`).join(', ') : 'None'}</p>
        <p><strong>Threat Confidence:</strong> ${chain.threat_confidence_score || 'N/A'}</p>
      `;
    } else {
      scamchainCard.style.display = 'none';
    }

    // Evidence Pack
    const evidencePack = data.evidence_pack || {};
    const evidenceItems = evidencePack.evidence_items || [];
    const evidenceHtml = evidenceItems.length > 0
      ? evidenceItems.map(item => `
        <div class="evidence-item">
          <div class="evidence-id">ID: ${item.evidence_id || 'N/A'}</div>
          <div class="evidence-description">${item.description || item.label || 'No description'}</div>
        </div>
      `).join('')
      : '<p>No evidence items recorded.</p>';
    document.getElementById('evidence-pack').innerHTML = evidenceHtml;
  }

  displayError(data) {
    this.noResponseState.style.display = 'none';
    this.responseContainer.style.display = 'block';

    const errorCard = document.getElementById('error-card');
    errorCard.style.display = 'block';
    document.getElementById('error-message').textContent = data.error_code || 'UNKNOWN_ERROR';
    document.getElementById('error-details').textContent = data.message || data.details?.[0] || 'No details available.';

    // Hide success cards
    document.getElementById('risk-card').style.display = 'none';
    document.getElementById('protection-decision-content').innerHTML = '';
    document.getElementById('mismatch-card').style.display = 'none';
    document.getElementById('scamchain-card').style.display = 'none';
  }

  displayNetworkError(errorMessage) {
    this.noResponseState.style.display = 'none';
    this.responseContainer.style.display = 'block';

    const errorCard = document.getElementById('error-card');
    errorCard.style.display = 'block';
    document.getElementById('error-message').textContent = 'NETWORK_ERROR';
    document.getElementById('error-details').textContent = `Could not connect to API: ${errorMessage}\n\nMake sure the RefGuard server is running on http://localhost:3000`;
  }

  toggleRawJson() {
    const jsonElement = document.getElementById('raw-json');
    const isHidden = jsonElement.style.display === 'none';
    jsonElement.style.display = isHidden ? 'block' : 'none';
    this.toggleJsonBtn.textContent = isHidden ? 'Hide' : 'Show';
  }
}

// Initialize the demo when the page loads
document.addEventListener('DOMContentLoaded', () => {
  new RefGuardDemo();
});
