const fs = require('fs');
let css = fs.readFileSync('integration/demo/public/style.css', 'utf8');

const desktopMedia = `

/* Desktop Layout Enhancements */
@media (min-width: 768px) {
  .container {
    max-width: 1100px;
    padding: 2rem;
  }
  
  #workspaceGrid {
    display: grid !important;
    grid-template-columns: 400px 1fr;
    grid-template-rows: auto 1fr;
    gap: 2rem;
  }

  #presetsSection {
    grid-column: 1;
    grid-row: 1;
  }

  #inputPanel {
    grid-column: 1;
    grid-row: 2;
    position: sticky;
    top: 6rem;
  }

  #resultsPanel {
    grid-column: 2;
    grid-row: 1 / span 2;
  }
  
  /* Transform Bottom Nav to Top Right Menu */
  .bottom-nav {
    position: fixed;
    top: 0;
    right: 0;
    bottom: auto;
    left: auto;
    background: transparent;
    border-top: none;
    padding: 1rem 1.5rem;
    gap: 1.5rem;
    z-index: 100;
    backdrop-filter: none;
  }
  
  .nav-btn {
    flex-direction: row;
    font-size: 0.9rem;
  }
  
  .nav-btn.active {
    background: rgba(255,255,255,0.1);
    padding: 0.5rem 1rem;
    border-radius: 20px;
  }
  
  body {
    padding-bottom: 0;
  }
}
`;

// Remove previous desktop patch if I accidentally ran it
if (!css.includes('Desktop Layout Enhancements')) {
  css += desktopMedia;
  fs.writeFileSync('integration/demo/public/style.css', css);
}
