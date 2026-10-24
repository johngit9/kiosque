// Génère les images du logo dans assets/ (node scripts/make-icons.js, Playwright requis).
const { chromium } = require('playwright');
const grad = `<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0e7c7b"/><stop offset="1" stop-color="#173a63"/></linearGradient></defs>`;
// Glyphe dessiné dans une grille 64 : K + point de signal
const glyph = (s=1, dx=0, dy=0) => `<g transform="translate(${dx} ${dy}) scale(${s})"><path d="M20 16v32M21 33l15-17M27 27l12 21" stroke="#fff" stroke-width="6" stroke-linecap="round" fill="none"/><circle cx="46" cy="18" r="4" fill="#7cf0c8"/></g>`;
const svgs = {
  "icon-only": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="1024" height="1024">${grad}<rect width="64" height="64" fill="url(#g)"/>${glyph()}</svg>`,
  "icon-background": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="1024" height="1024">${grad}<rect width="64" height="64" fill="url(#g)"/></svg>`,
  // Zone sûre des icônes adaptatives Android : on réduit le glyphe à 62 % au centre
  "icon-foreground": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="1024" height="1024">${glyph(0.62, 32-32*0.62, 32-32*0.62)}</svg>`,
  "splash": `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 640" width="2732" height="2732">${grad}<rect width="640" height="640" fill="url(#g)"/>${glyph(2.2, 320-32*2.2, 300-32*2.2)}<text x="320" y="420" text-anchor="middle" font-family="Georgia, serif" font-weight="700" font-size="40" fill="#fff">Kiosque</text></svg>`,
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
