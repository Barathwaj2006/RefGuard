const fs = require('fs');
let html = fs.readFileSync('integration/demo/public/index.html', 'utf8');

const oldHero = `<div class="confidence-badge">
              <span id="riskScoreVal">0</span>/100 Risk Score
            </div>`;

const newHero = `<div class="confidence-badge" style="display: flex; gap: 1rem; justify-content: center; margin-top: 0.5rem;">
              <span>Score: <strong id="riskScoreVal">0</strong>/100</span>
              <span style="border-left: 1px solid rgba(255,255,255,0.3); padding-left: 1rem;">Confidence: <strong id="riskConfidenceVal">0</strong>%</span>
            </div>
            <div id="heroImmediateAction" style="margin-top: 1.5rem; font-weight: 600; padding: 0.75rem; background: rgba(0,0,0,0.2); border-radius: 8px; display: none;"></div>`;

if (html.includes(oldHero)) {
  html = html.replace(oldHero, newHero);
  fs.writeFileSync('integration/demo/public/index.html', html);
}
