// Génère les images du logo dans assets/ (node scripts/make-icons.js, Playwright requis).
const { chromium } = require('playwright');
const BG = "#13233d";
const grad = "";
// Logo : journal blanc posé sur une page corail, sur fond bleu nuit (grille 64)
const glyph = (s=1, dx=0, dy=0) => `<g transform="translate(${dx} ${dy}) scale(${s})"><rect x="21" y="13" width="29" height="35" rx="3.5" fill="#ff5a3c" transform="rotate(8 35 30)"/><rect x="14" y="16" width="31" height="37" rx="3.5" fill="#fff"/><rect x="18.5" y="21" width="22" height="4.5" rx="1.2" fill="#13233d"/><rect x="18.5" y="29" width="10" height="9" rx="1.2" fill="#ff5a3c"/><rect x="31" y="29" width="9.5" height="2.2" rx="1.1" fill="#b9c3d1"/><rect x="31" y="33" width="9.5" height="2.2" rx="1.1" fill="#b9c3d1"/><rect x="31" y="37" width="7" height="2.2" rx="1.1" fill="#b9c3d1"/><rect x="18.5" y="42" width="22" height="2.2" rx="1.1" fill="#b9c3d1"/><rect x="18.5" y="46" width="16" height="2.2" rx="1.1" fill="#b9c3d1"/></g>`;
const svgs = {
  "icon-only": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="1024" height="1024">${grad}<rect width="64" height="64" fill="${BG}"/>${glyph()}</svg>`,
  "icon-background": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="1024" height="1024">${grad}<rect width="64" height="64" fill="${BG}"/></svg>`,
  // Zone sûre des icônes adaptatives Android : on réduit le glyphe à 62 % au centre
  "icon-foreground": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="1024" height="1024">${glyph(0.62, 32-32*0.62, 32-32*0.62)}</svg>`,
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
