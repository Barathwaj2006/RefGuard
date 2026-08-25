const fs = require('fs');
let html = fs.readFileSync('integration/demo/public/index.html', 'utf8');

function extractNode(idStr) {
  // Finds a div or details by id and extracts the entire node block.
  // This is a rough but effective regex for these specific nodes given their structure.
  // Actually, string manipulation is safer since we know the HTML format.
  let startIdx = html.indexOf(idStr);
  if (startIdx === -1) return null;
  
  // Walk backwards to the start of the <div or <details tag
  while (startIdx > 0 && html.substring(startIdx - 4, startIdx) !== '<div' && html.substring(startIdx - 8, startIdx) !== '<details') {
    startIdx--;
  }
  
  // Find the end tag
  let endTag = html.substring(startIdx, startIdx + 8).startsWith('<details') ? '</details>' : '</div>';
  
  // We need to handle nested divs! A simple regex won't work for nested divs.
  let tagCount = 0;
  let i = startIdx;
  let inString = false;
  
  while (i < html.length) {
    if (html.substring(i, i+4) === '<div') { tagCount++; i += 3; }
    else if (html.substring(i, i+5) === '</div>') { tagCount--; i += 5; }
    else if (html.substring(i, i+8) === '<details') { tagCount++; i += 7; }
    else if (html.substring(i, i+10) === '</details>') { tagCount--; i += 9; }
    
    if (tagCount === 0) {
      break;
    }
    i++;
  }
  
  const content = html.substring(startIdx, i + 1);
  html = html.replace(content, `<!-- EXTRACTED ${idStr} -->`);
  return content;
}

// Ensure we don't accidentally do this multiple times if the script runs twice
if (!html.includes('<!-- REORDERED BLOCK START -->')) {
  const explanationCard = extractNode('id="explanationCard"');
  const scamChainCard = extractNode('id="scamChainCard"');
  const objectiveCard = extractNode('id="objectiveCard"');
  const nextStepCard = extractNode('id="nextStepCard"');
  const incidentCard = extractNode('id="incidentCard"');
  const evidenceCard = extractNode('id="evidenceCard"');
  const feedbackCard = extractNode('id="feedbackCard"');
  const newScanBtn = extractNode('id="newScanBtn"');

  const orderedContent = `
<!-- REORDERED BLOCK START -->
${explanationCard || ''}
${scamChainCard || ''}
${objectiveCard || ''}
${nextStepCard || ''}
${incidentCard || ''}
${evidenceCard || ''}
${feedbackCard || ''}
${newScanBtn || ''}
<!-- REORDERED BLOCK END -->
`;

  // We place it where explanationCard used to be
  html = html.replace('<!-- EXTRACTED id="explanationCard" -->', orderedContent);
  
  // Remove the other placeholders
  html = html.replace(/<!-- EXTRACTED id=".*?" -->\n?/g, '');
  
  fs.writeFileSync('integration/demo/public/index.html', html);
}
