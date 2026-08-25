const fs = require('fs');
let html = fs.readFileSync('integration/demo/public/index.html', 'utf8');

const targetHeader = `<header class="top-bar">
    <div class="brand">
      <span class="shield-icon" style="font-size: 1.8rem;">🛡️</span>
      <div class="brand-text">
        <h1>RefGuard</h1>
        <span class="brand-subtitle">Ambient Scam Protection</span>
      </div>
    </div>
  </header>`;

const newHeader = `<header class="top-bar">
    <div class="brand">
      <svg viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M50 8.33331L16.6667 23.3333V45.8333C16.6667 67.125 30.9584 86.8333 50 91.6666C69.0417 86.8333 83.3334 67.125 83.3334 45.8333V23.3333L50 8.33331Z" fill="#0066FF"/>
        <path d="M50 8.33331L16.6667 23.3333V45.8333C16.6667 67.125 30.9584 86.8333 50 91.6666V8.33331Z" fill="#06B6D4"/>
        <path d="M43.75 62.5L29.1667 47.9167L35.0417 42.0417L43.75 50.7083L64.9583 29.5L70.8333 35.4167L43.75 62.5Z" fill="white"/>
      </svg>
      <div>
        <div style="font-size: 1.5rem; font-weight: 800; line-height: 1.2;">RefGuard</div>
      </div>
    </div>
    <button class="settings-btn" title="Settings">⚙️</button>
  </header>`;

if (html.includes('<header class="top-bar">')) {
  // We use regex to replace because the exact whitespace might differ
  html = html.replace(/<header class="top-bar">[\s\S]*?<\/header>/, newHeader);
  fs.writeFileSync('integration/demo/public/index.html', html);
}
