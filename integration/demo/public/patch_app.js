const fs = require('fs');

let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

const insertObjective = `
  // Attacker Objective
  const objectiveCard = document.getElementById('objectiveCard');
  const objectiveDesc = document.getElementById('attackerObjectiveDesc');
  if (objectiveCard && objectiveDesc) {
    if (severity === 'CRITICAL' || severity === 'HIGH') {
      objectiveCard.style.display = 'block';
      // Attempt to extract objective from explanation or summary
      let objText = risk.human_explanation || decision.detected_summary || 'Convince you to authorize a fraudulent payment.';
      
      // If it's a mismatch, the objective is usually tricking the payment direction
      if (mismatch && mismatch.status === 'DETECTED') {
        objText = 'Get you to authorize a payment by making you believe you are receiving a refund or reward.';
      }
      objectiveDesc.innerText = objText;
    } else {
      objectiveCard.style.display = 'none';
    }
  }

  // Next Likely Step
  const nextStepCard = document.getElementById('nextStepCard');
  const nextStepDesc = document.getElementById('nextStepDesc');
  let predictedNext = null;
  const nodes = scamChain.nodes || [];
  if (nodes.length > 2) {
      // Index 2+ are considered predictions in the current frontend logic
      predictedNext = nodes[2];
  }
  
  if (nextStepCard && nextStepDesc) {
    if (predictedNext) {
      nextStepCard.style.display = 'block';
      let typeText = predictedNext.node_type ? predictedNext.node_type.replace(/_/g, ' ') : 'ACTION';
      nextStepDesc.innerText = 'The attacker will likely try to execute a ' + typeText + ' (' + (predictedNext.entity_reference || predictedNext.node_id) + '). Do not proceed.';
    } else {
      nextStepCard.style.display = 'none';
    }
  }
`;

// Insert the new logic right before `// Scam Chain`
app = app.replace('  // Scam Chain', insertObjective + '\n  // Scam Chain');

fs.writeFileSync('integration/demo/public/app.js', app);
