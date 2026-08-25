const fs = require('fs');
let html = fs.readFileSync('integration/demo/public/index.html', 'utf8');

const adaptiveIntelHtml = `
            <!-- Adaptive Intelligence Card -->
            <div class="explanation-card" id="adaptiveIntelCard" style="display: none; border-left: 4px solid var(--accent-blue);">
              <h3 style="color: var(--accent-blue);">Threat Profile</h3>
              <div class="intel-grid" style="display: grid; grid-template-columns: 1fr; gap: 0.75rem; margin-top: 1rem;">
                <div id="aiScamTypeBox" style="display: none;">
                  <strong style="color: var(--text-secondary); font-size: 0.8rem; text-transform: uppercase;">Scam Type</strong>
                  <div id="aiScamType" style="font-weight: 500;"></div>
                </div>
                <div id="aiCurrentStageBox" style="display: none;">
                  <strong style="color: var(--text-secondary); font-size: 0.8rem; text-transform: uppercase;">Current Stage</strong>
                  <div id="aiCurrentStage" style="font-weight: 500;"></div>
                </div>
                <div id="aiObjectiveBox" style="display: none;">
                  <strong style="color: var(--text-secondary); font-size: 0.8rem; text-transform: uppercase;">Attacker Objective</strong>
                  <div id="aiObjective"></div>
                </div>
                <div id="aiUserRiskBox" style="display: none;">
                  <strong style="color: var(--text-secondary); font-size: 0.8rem; text-transform: uppercase;">User Risk</strong>
                  <div id="aiUserRisk"></div>
                </div>
                <div id="aiNextStepBox" style="display: none;">
                  <strong style="color: var(--text-secondary); font-size: 0.8rem; text-transform: uppercase;">What May Happen Next</strong>
                  <div id="aiNextStep"></div>
                </div>
                <div id="aiRecommendedActionBox" style="display: none;">
                  <strong style="color: var(--text-secondary); font-size: 0.8rem; text-transform: uppercase;">Recommended Action</strong>
                  <div id="aiRecommendedAction" style="font-weight: 500; color: var(--color-critical);"></div>
                </div>
              </div>
            </div>
`;

if (!html.includes('id="adaptiveIntelCard"')) {
  // Replace objectiveCard and nextStepCard with this unified card
  html = html.replace(/<div class="explanation-card" id="objectiveCard"[\s\S]*?<\/div>/, '');
  html = html.replace(/<div class="explanation-card" id="nextStepCard"[\s\S]*?<\/div>/, adaptiveIntelHtml);
  fs.writeFileSync('integration/demo/public/index.html', html);
}
