const fs = require('fs');
let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

const targetStr = `    if (model.chainNodes.length > 0) {
      chainCard.style.display = 'block';`;

const replacement = `    if (model.chainNodes.length > 0 && model.severity !== 'LOW') {
      chainCard.style.display = 'block';`;

if (app.includes(targetStr)) {
  app = app.replace(targetStr, replacement);
  fs.writeFileSync('integration/demo/public/app.js', app);
}
