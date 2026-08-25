const fs = require('fs');
let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

app = app.replace("if (model.severity === 'CRITICAL') severityTag.innerText = 'CRITICAL SCAM RISK';", 
                  "if (model.severity === 'CRITICAL') severityTag.innerText = 'CRITICAL RISK';");

const appendLogic = `
  const confVal = document.getElementById('riskConfidenceVal');
  if (confVal) confVal.innerText = Math.round((model.confidence || 0) * 100);
  
  const heroAction = document.getElementById('heroImmediateAction');
  if (heroAction) {
    if (model.action && model.severity !== 'LOW') {
      heroAction.innerText = 'Action: ' + model.action;
      heroAction.style.display = 'block';
    } else {
      heroAction.style.display = 'none';
    }
  }
`;

app = app.replace('if (scoreVal) scoreVal.innerText = model.score;', 'if (scoreVal) scoreVal.innerText = model.score;\n' + appendLogic);
fs.writeFileSync('integration/demo/public/app.js', app);
