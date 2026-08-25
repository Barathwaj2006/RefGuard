const fs = require('fs');

let html = fs.readFileSync('integration/demo/public/index.html', 'utf8');

// Insert new sections into the Explanation Card
const insertIndex = html.indexOf('<!-- Scam Chain -->');

const newSections = `
          <!-- Attacker Objective -->
          <div class="explanation-card" id="objectiveCard" style="display: none; border-left: 4px solid var(--color-high);">
            <h3 style="color: var(--color-high);">What is the attacker trying to do?</h3>
            <p id="attackerObjectiveDesc" style="font-size: 0.95rem; color: var(--text-primary); margin-top: 0.5rem;"></p>
          </div>

          <!-- Next Likely Step -->
          <div class="explanation-card" id="nextStepCard" style="display: none; border-left: 4px solid var(--text-muted);">
            <h3 style="color: var(--text-muted);">What may happen next</h3>
            <p id="nextStepDesc" style="font-size: 0.95rem; color: var(--text-primary); margin-top: 0.5rem;"></p>
          </div>

          `;

html = html.slice(0, insertIndex) + newSections + html.slice(insertIndex);
fs.writeFileSync('integration/demo/public/index.html', html);
