const fs = require('fs');
let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

const backButtonLogic = `
// --- CAPACITOR ANDROID BACK BUTTON SUPPORT ---
if (window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.App) {
  window.Capacitor.Plugins.App.addListener('backButton', () => {
      const scanView = document.getElementById('workspaceGrid');
      const resultsPanel = document.getElementById('resultsPanel');
      const historyView = document.getElementById('historyView');
      const intelView = document.getElementById('intelView');

      if (historyView && historyView.style.display !== 'none') {
        switchTab('scan', document.querySelector('.nav-item:nth-child(1)'));
        return;
      }
      
      if (intelView && intelView.style.display !== 'none') {
        switchTab('scan', document.querySelector('.nav-item:nth-child(1)'));
        return;
      }
      
      if (scanView && scanView.style.display !== 'none' && resultsPanel && resultsPanel.style.display !== 'none') {
        clearScanner();
        return;
      }

      window.Capacitor.Plugins.App.exitApp();
  });
}
`;

// 1. Configure API Base URL
const configSetup = `
// --- REFGUARD ENVIRONMENT CONFIGURATION ---
const ENV = 'development'; // Change to 'production' for final APK if real backend deployed
const API_BASE_URL = ENV === 'production' 
  ? 'https://refguard-api-hackathon-demo.onrender.com' 
  : 'http://10.0.2.2:3000'; // 10.0.2.2 is the Android Emulator alias for localhost
`;

// Only add if not already added
if (!app.includes('API_BASE_URL')) {
  app = configSetup + '\n\n' + app + '\n\n' + backButtonLogic;

  app = app.replace(/fetch\('\/api\/v1\/scan'/g, "fetch(API_BASE_URL + '/api/v1/scan'");
  app = app.replace(/fetch\('\/api\/v1\/incident\/recommendation'/g, "fetch(API_BASE_URL + '/api/v1/incident/recommendation'");
  app = app.replace(/fetch\('\/api\/v1\/report'/g, "fetch(API_BASE_URL + '/api/v1/report'");
  app = app.replace(/fetch\('\/api\/v1\/feedback'/g, "fetch(API_BASE_URL + '/api/v1/feedback'");
  app = app.replace(/fetch\('\/api\/v1\/intel\/trending'/g, "fetch(API_BASE_URL + '/api/v1/intel/trending'");
  app = app.replace(/fetch\('\/api\/v1\/intel\/reports'/g, "fetch(API_BASE_URL + '/api/v1/intel/reports'");
  app = app.replace(/fetch\('\/api\/v1\/report'/g, "fetch(API_BASE_URL + '/api/v1/report'"); // just in case

  fs.writeFileSync('integration/demo/public/app.js', app);
}
