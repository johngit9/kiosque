// Génère les images du logo dans assets/ (node scripts/make-icons.js, Playwright requis).
const { chromium } = require('playwright');
const BG = "#13233d";
const grad = "";
// Logo : journal blanc posé sur une page corail, formes épaisses pour rester lisible en petit, sur fond bleu nuit (grille 64)
const glyph = (s=1, dx=0, dy=0) => `<g transform="translate(${dx} ${dy}) scale(${s})"><rect x="22" y="8" width="32" height="42" rx="5" fill="#ff5a3c"/><rect x="10" y="14" width="34" height="42" rx="5" fill="#ffffff"/><rect x="15" y="20" width="24" height="6" rx="2" fill="#13233d"/><rect x="15" y="30" width="11" height="11" rx="2" fill="#ff5a3c"/><rect x="29" y="31" width="10" height="3.6" rx="1.8" fill="#8a96a8"/><rect x="29" y="37.4" width="10" height="3.6" rx="1.8" fill="#8a96a8"/><rect x="15" y="45" width="24" height="3.6" rx="1.8" fill="#8a96a8"/></g>`;
const svgs = {
  "icon-only": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="1024" height="1024">${grad}<rect width="64" height="64" fill="${BG}"/>${glyph()}</svg>`,
  "icon-background": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="1024" height="1024">${grad}<rect width="64" height="64" fill="${BG}"/></svg>`,
  // Icône adaptative Android : glyphe agrandi (72 %) pour rester lisible sur l'écran d'accueil
  "icon-foreground": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="1024" height="1024">${glyph(0.72, 32-32*0.72, 32-32*0.72)}</svg>`,
  "splash": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 640" width="2732" height="2732">${grad}<rect width="640" height="640" fill="${BG}"/>${glyph(2.2, 320-32*2.2, 300-32*2.2)}<text x="320" y="420" text-anchor="middle" font-family="Caladea, Georgia, serif" font-weight="700" font-size="40" fill="#f6f1e7">Kiosque</text></svg>`,
};
svgs["splash-dark"] = svgs.splash;
(async () => {
  const b = await chromium.launch();
  for (const [name, svg] of Object.entries(svgs)) {
    const size = name.startsWith("splash") ? 2732 : 1024;
    const scale = size > 2000 ? 2 : 1; const css = size / scale;
    const p = await b.newPage({ viewport: { width: css, height: css }, deviceScaleFactor: scale });
    await p.setContent(`<html><body style="margin:0;background:transparent">${svg.replace(/width="\d+" height="\d+"/, `width="${css}" height="${css}"`)}</body></html>`);
    await p.screenshot({ path: `assets/${name}.png`, omitBackground: true, clip: { x: 0, y: 0, width: css, height: css } });
    await p.close();
  }
  await b.close();
})();
