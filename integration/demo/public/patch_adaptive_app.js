const fs = require('fs');
let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

const oldLogic = `  // Attacker Objective
  const objectiveCard = document.getElementById('objectiveCard');
  const objectiveDesc = document.getElementById('attackerObjectiveDesc');
  if (objectiveCard && objectiveDesc) {
    if ((model.severity === 'CRITICAL' || model.severity === 'HIGH') && model.adaptiveIntel.objective) {
      objectiveCard.style.display = 'block';
      objectiveDesc.innerText = model.adaptiveIntel.objective;
    } else {
      objectiveCard.style.display = 'none';
    }
  }

  // Next Likely Step
  const nextStepCard = document.getElementById('nextStepCard');
  const nextStepDesc = document.getElementById('nextStepDesc');
  
  if (nextStepCard && nextStepDesc) {
    // If backend provides a predicted next step, use it, else synthesize from scam chain
    let nextText = model.adaptiveIntel.nextStep;
    if (!nextText && model.chainNodes.length > 2) {
      const predictedNext = model.chainNodes[2];
      let typeText = predictedNext.node_type ? predictedNext.node_type.replace(/_/g, ' ') : 'ACTION';
      nextText = 'The attacker will likely try to execute a ' + typeText + ' (' + (predictedNext.entity_reference || predictedNext.node_id) + '). Do not proceed.';
    }

    if (nextText && (model.severity === 'CRITICAL' || model.severity === 'HIGH')) {
      nextStepCard.style.display = 'block';
      nextStepDesc.innerText = nextText;
    } else {
      nextStepCard.style.display = 'none';
    }
  }`;

const newLogic = `  // Unified Adaptive Intelligence Card
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
  }`;

if (app.includes('// Attacker Objective')) {
  app = app.replace(oldLogic, newLogic);
  fs.writeFileSync('integration/demo/public/app.js', app);
}
