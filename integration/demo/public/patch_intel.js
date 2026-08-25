const fs = require('fs');

let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

const intelStart = app.indexOf('async function loadIntel() {');
const toastStart = app.indexOf('// Toast System');

if (intelStart !== -1 && toastStart !== -1) {
const newIntel = `async function loadIntel() {
  const trendingList = document.getElementById('trendingList');
  const reportsList = document.getElementById('reportsList');

  if (trendingList) trendingList.innerHTML = '<div class="intel-card"><p>Loading trending signatures...</p></div>';
  if (reportsList) reportsList.innerHTML = '<div class="intel-card"><p>Loading recent reports...</p></div>';

  try {
    const [trendRes, reportRes] = await Promise.all([
      fetch('/api/v1/intel/trending').catch(() => null),
      fetch('/api/v1/intel/reports').catch(() => null)
    ]);

    if (trendingList) {
      trendingList.innerHTML = '';
      if (trendRes && trendRes.ok) {
        const data = await trendRes.json();
        const items = data.trending_indicators || [];
        if (items.length > 0) {
          items.forEach(ind => {
            const div = document.createElement('div');
            div.className = 'intel-card';
            div.innerHTML = '<h4 style="color: var(--color-critical);">' + escapeHtml(ind.indicator) + '</h4>' +
                             '<p>Severity: <strong>' + escapeHtml(ind.severity || 'CRITICAL') + '</strong> | Sightings: ' + (ind.sightings || ind.reportCount || 1) + '</p>';
            trendingList.appendChild(div);
          });
        } else {
          trendingList.innerHTML = '<div class="intel-card"><p>No trending threats found in registry.</p></div>';
        }
      } else {
        trendingList.innerHTML = '<div class="intel-card"><p>Threat intelligence endpoint unavailable.</p></div>';
      }
    }

    if (reportsList) {
      reportsList.innerHTML = '';
      if (reportRes && reportRes.ok) {
        const data = await reportRes.json();
        const reports = data.recent_reports || data.reports || [];
        if (reports.length > 0) {
          reports.forEach(rep => {
            const div = document.createElement('div');
            div.className = 'intel-card';
            const dateStr = rep.submission_timestamp ? new Date(rep.submission_timestamp).toLocaleString() : 'Recent';
            div.innerHTML = '<h4>' + escapeHtml(rep.reported_indicator || 'Redacted') + '</h4>' +
                             '<p>Category: <strong>' + escapeHtml(rep.report_category || 'UPI_FRAUD') + '</strong> | ' + escapeHtml(dateStr) + '</p>';
            reportsList.appendChild(div);
          });
        } else {
          reportsList.innerHTML = '<div class="intel-card"><p>No community reports filed yet.</p></div>';
        }
      } else {
        reportsList.innerHTML = '<div class="intel-card"><p>Community reports endpoint unavailable.</p></div>';
      }
    }

  } catch (err) {
    if (trendingList) trendingList.innerHTML = '<div class="intel-card"><p style="color: var(--color-critical);">Failed to load trending threats.</p></div>';
    if (reportsList) reportsList.innerHTML = '<div class="intel-card"><p style="color: var(--color-critical);">Failed to load recent reports.</p></div>';
    showToast('Failed to load global intelligence feed', 'error');
  }
}

`;
app = app.substring(0, intelStart) + newIntel + app.substring(toastStart);
fs.writeFileSync('integration/demo/public/app.js', app);
}
