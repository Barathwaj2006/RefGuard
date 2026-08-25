const fs = require('fs');
let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

const oldRender = `function renderHistory() {
  const container = document.getElementById('historyListContainer');
  if (!container) return;
  
  let history = [];
  try {
    const stored = localStorage.getItem(HISTORY_STORAGE_KEY);
    if (stored) history = JSON.parse(stored);
  } catch(e) {
    history = [];
  }

  container.innerHTML = '';
  
  if (history.length === 0) {
    container.innerHTML = \`
      <div class="empty-state">
        <div class="empty-icon">📭</div>
        <h4>No Scan History Yet</h4>
        <p>Run scans in the Scanner tab to build your local privacy-safe threat history.</p>
      </div>
    \`;
    return;
  }

  history.forEach(item => {`;

const newRender = `function renderHistory() {
  const container = document.getElementById('historyListContainer');
  if (!container) return;
  
  // LOADING STATE
  container.innerHTML = '<div style="padding:2rem; text-align:center; color:var(--text-muted);">Loading history securely...</div>';
  
  setTimeout(() => {
    let history = [];
    try {
      const stored = localStorage.getItem(HISTORY_STORAGE_KEY);
      if (stored) history = JSON.parse(stored);
    } catch(e) {
      // ERROR STATE
      container.innerHTML = '<div style="padding:2rem; text-align:center; color:var(--color-critical);">Failed to load history data.</div>';
      return;
    }

    container.innerHTML = '';
    
    // EMPTY STATE
    if (history.length === 0) {
      container.innerHTML = \`
        <div class="empty-state">
          <div class="empty-icon">📭</div>
          <h4>No Scan History Yet</h4>
          <p>Run scans in the Scanner tab to build your local privacy-safe threat history.</p>
        </div>
      \`;
      return;
    }

    // POPULATED STATE
    history.forEach(item => {`;

if (app.includes('if (!container) return;')) {
  app = app.replace(oldRender, newRender);
  
  // Also we need to close the settimeout bracket inside renderHistory
  const closingBraces = `    });
  }
`;
  app = app.replace("    container.appendChild(div);\n  });\n}\n\nfunction clearHistory()", "    container.appendChild(div);\n  });\n  }, 200);\n}\n\nfunction clearHistory()");

  fs.writeFileSync('integration/demo/public/app.js', app);
}
