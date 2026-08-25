const fs = require('fs');
let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

const oldRender = `        const div = document.createElement('div');
        div.className = 'chain-node ' + cls;
        div.innerHTML = '<div class="chain-dot"></div>' +
          '<div class="chain-node-title">' + escapeHtml(node.entity_reference || node.node_id) + '</div>' +
          '<div class="chain-node-desc">' + escapeHtml(node.node_type) + ' ' + icon + ' ' + status + '</div>';
        chainContainer.appendChild(div);`;

const newRender = `        const div = document.createElement('div');
        div.className = 'chain-node ' + cls;
        
        let confidenceHtml = '';
        if (node.confidence !== undefined) {
           confidenceHtml = \`<span style="margin-left: 0.5rem; font-size: 0.8em; opacity: 0.8;">(\${Math.round(node.confidence * 100)}% confidence)</span>\`;
        }
        
        let provenanceHtml = '';
        if (node.provenance) {
           provenanceHtml = \`<div style="font-size: 0.8em; color: var(--text-muted); margin-top: 0.25rem;">Source: \${escapeHtml(node.provenance)}</div>\`;
        }
        
        let evidenceHtml = '';
        if (node.evidence_references && node.evidence_references.length > 0) {
           evidenceHtml = \`<div style="font-size: 0.8em; color: var(--text-muted); margin-top: 0.1rem;">Evidence Ref: \${escapeHtml(node.evidence_references.join(', '))}</div>\`;
        }

        div.innerHTML = \`
          <div class="chain-dot"></div>
          <div class="chain-node-title" style="margin-bottom:0.2rem;">\${escapeHtml(node.entity_reference || node.node_id)}</div>
          <div class="chain-node-desc">
            <strong>\${icon} \${status}</strong> &mdash; \${escapeHtml(node.node_type)}
            \${confidenceHtml}
            \${provenanceHtml}
            \${evidenceHtml}
          </div>
        \`;
        chainContainer.appendChild(div);`;

if (app.includes('chain-node-title')) {
  app = app.replace(oldRender, newRender);
  fs.writeFileSync('integration/demo/public/app.js', app);
}
