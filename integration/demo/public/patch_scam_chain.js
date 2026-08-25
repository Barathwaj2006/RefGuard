const fs = require('fs');
let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

const oldScamChainLogic = `        // Distinguish OBSERVED vs INFERRED vs PREDICTED
        // Index 0 is typically OBSERVED. Index 1 INFERRED. Index 2+ PREDICTED.
        let status = 'OBSERVED';
        let icon = '✓';
        let cls = 'detected';
        
        if (idx === 1) {
          status = 'INFERRED';
          icon = '◉';
          cls = 'inferred';
        } else if (idx > 1) {
          status = 'PREDICTED';
          icon = '→';
          cls = 'predicted';
        }`;

const newScamChainLogic = `        // Consume explicit backend semantics for node status
        const rawStatus = (node.status || node.state || node.node_status || node.observation_status || '').toUpperCase();
        
        let status = 'UNVERIFIED';
        let icon = '⁈';
        let cls = 'inferred';
        
        if (rawStatus === 'OBSERVED') {
          status = 'OBSERVED';
          icon = '✓';
          cls = 'detected';
        } else if (rawStatus === 'INFERRED') {
          status = 'INFERRED';
          icon = '◉';
          cls = 'inferred';
        } else if (rawStatus === 'PREDICTED') {
          status = 'PREDICTED';
          icon = '→';
          cls = 'predicted';
        }`;

if (app.includes(oldScamChainLogic)) {
  app = app.replace(oldScamChainLogic, newScamChainLogic);
  fs.writeFileSync('integration/demo/public/app.js', app);
}
