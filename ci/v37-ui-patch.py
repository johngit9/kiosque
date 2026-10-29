from pathlib import Path
p=Path('www/index.html')
s=p.read_text(encoding='utf-8')
css=r'''<style id="v37-validated-ui">
:root{--k-text-scale:1;--k-menu-scale:.88}
body{font-size:calc(15px * var(--k-text-scale))!important}
header.top{padding-block:6px!important;gap:8px!important}.brand{font-size:22px!important}.brand svg{width:38px!important;height:38px!important}.round{width:34px!important;height:34px!important}.search{padding-top:7px!important;padding-bottom:7px!important}
.menubar{padding-block:5px!important}.menubar button{padding:calc(5px * var(--k-menu-scale)) calc(10px * var(--k-menu-scale))!important;font-size:calc(13px * var(--k-menu-scale) + 2px)!important}.submenu button{padding:7px 10px 6px!important}
.layout{padding-top:12px!important;gap:18px!important}.card,.article,.item{border-radius:14px!important}img{max-width:100%;height:auto}.card img,.article img,.item img,.thumb img{object-fit:cover!important;object-position:center!important}
/* briefing compact */
.briefing,.brief,.briefcard,[class*="briefing"]{min-height:0!important;padding:16px 18px!important;margin-block:10px!important}.briefing h1,.briefing h2,.brief h1,.brief h2,[class*="briefing"] h1,[class*="briefing"] h2{font-size:clamp(22px,6vw,31px)!important;line-height:1.05!important;margin:4px 0 8px!important}.briefing p,.brief p,[class*="briefing"] p{margin:4px 0 10px!important;line-height:1.3!important}.briefing button,.brief button,[class*="briefing"] button{padding:8px 14px!important}
/* barre basse compacte */
.bottomnav,.bottom-nav,.tabbar,.tabs-bottom,[class*="bottomnav"],[class*="bottom-nav"]{min-height:56px!important;height:auto!important;padding:5px 8px max(5px,env(safe-area-inset-bottom))!important}.bottomnav button,.bottom-nav button,.tabbar button,[class*="bottomnav"] button{padding:4px 8px!important;font-size:12px!important}
/* images d'articles */
figure img,.hero img,.cover img,[class*="image"] img{width:100%;max-height:48vh;object-fit:cover!important;border-radius:12px}
/* panneau V37 */
#k-v37-settings{position:fixed;inset:0;z-index:999999;background:rgba(5,12,22,.62);display:none;align-items:flex-end}#k-v37-settings.open{display:flex}#k-v37-sheet{width:100%;max-height:82vh;overflow:auto;background:var(--surface,#fff);color:var(--ink,#14202b);border-radius:22px 22px 0 0;padding:18px 18px calc(18px + env(safe-area-inset-bottom));box-shadow:0 -12px 40px #0005}#k-v37-sheet h2{margin:0 0 14px;font:700 24px/1.1 var(--f-head,system-ui)}.kvrow{display:grid;grid-template-columns:1fr auto;align-items:center;gap:12px;padding:12px 0;border-bottom:1px solid var(--line,#ddd)}.kvrow label{font-weight:600}.kvrow small{display:block;color:var(--muted,#667);font-weight:400}.kvseg{display:flex;gap:5px}.kvseg button{border:1px solid var(--line,#ddd);background:var(--bg,#f5f6f7);border-radius:10px;padding:7px 9px}.kvseg button.on{background:var(--accent,#0b6e6e);color:white}.kvclose{width:100%;margin-top:16px;border:0;border-radius:12px;padding:11px;background:var(--accent,#0b6e6e);color:white;font-weight:700}
/* logos sources / recherche journal */
.src{min-height:38px}.src .klogo{width:26px;height:26px;border-radius:7px;object-fit:contain;background:white;flex:none}.src[data-name]{position:relative}
</style>'''
js=r'''<script id="v37-validated-js">
(()=>{const R=document.documentElement,LS=localStorage;
const apply=()=>{R.style.setProperty('--k-text-scale',LS.kText||'1');R.style.setProperty('--k-menu-scale',LS.kMenu||'.88')};apply();
const logoFor=n=>{const t=(n||'').trim();return t?`https://www.google.com/s2/favicons?domain=${encodeURIComponent(t)}&sz=64`:''};
function enhanceSources(){document.querySelectorAll('.src').forEach(b=>{if(b.querySelector('.klogo'))return;const txt=(b.textContent||'').trim();let host='';const m=txt.toLowerCase();const map={'le monde':'lemonde.fr','les echos':'lesechos.fr','le figaro':'lefigaro.fr','franceinfo':'franceinfo.fr','france info':'franceinfo.fr','01net':'01net.com','zdnet':'zdnet.fr','univers freebox':'universfreebox.com','light reading':'lightreading.com','clubic':'clubic.com','numerama':'numerama.com','bfm':'bfmtv.com','orange':'orange.com'};for(const k in map)if(m.includes(k)){host=map[k];break}if(host){const i=document.createElement('img');i.className='klogo';i.src=logoFor(host);i.alt='';b.prepend(i)}})}
function rejectConsent(root=document){const words=['tout rejeter','refuser tout','continuer sans accepter','reject all','refuse all'];root.querySelectorAll('button,a').forEach(e=>{const t=(e.textContent||'').trim().toLowerCase();if(words.some(w=>t===w||t.includes(w)))try{e.click()}catch(_){}})}
function sheet(){if(document.getElementById('k-v37-settings'))return;const d=document.createElement('div');d.id='k-v37-settings';d.innerHTML=`<div id="k-v37-sheet"><h2>Réglages d’affichage</h2><div class="kvrow"><div><label>Taille du texte</label><small>Articles et contenus</small></div><div class="kvseg" data-k="kText"><button data-v=".9">Petit</button><button data-v="1">Normal</button><button data-v="1.12">Grand</button></div></div><div class="kvrow"><div><label>Taille des menus</label><small>Navigation et catégories</small></div><div class="kvseg" data-k="kMenu"><button data-v=".78">Compact</button><button data-v=".88">Normal</button><button data-v="1">Grand</button></div></div><button class="kvclose">Terminé</button></div>`;document.body.appendChild(d);d.addEventListener('click',e=>{const b=e.target.closest('[data-v]');if(b){const g=b.parentElement,k=g.dataset.k;LS[k]=b.dataset.v;g.querySelectorAll('button').forEach(x=>x.classList.toggle('on',x===b));apply()}if(e.target===d||e.target.closest('.kvclose'))d.classList.remove('open')});
window.kiosqueDisplaySettings=()=>{d.classList.add('open');d.querySelectorAll('.kvseg').forEach(g=>g.querySelectorAll('button').forEach(b=>b.classList.toggle('on',b.dataset.v===(LS[g.dataset.k]||({kText:'1',kMenu:'.88'}[g.dataset.k])))))};
}
function addSettingsEntry(){const candidates=[...document.querySelectorAll('button,.btn,.round')];const existing=candidates.find(e=>/réglage|paramètre|settings/i.test(e.textContent||e.getAttribute('aria-label')||e.title||''));if(existing&&!existing.dataset.kv37){existing.dataset.kv37='1';existing.addEventListener('click',()=>setTimeout(()=>window.kiosqueDisplaySettings(),50))}}
const obs=new MutationObserver(()=>{enhanceSources();rejectConsent();addSettingsEntry()});obs.observe(document.documentElement,{subtree:true,childList:true});document.addEventListener('DOMContentLoaded',()=>{sheet();enhanceSources();rejectConsent();addSettingsEntry()});setTimeout(()=>{sheet();enhanceSources();addSettingsEntry()},800);
})();
</script>'''
if 'id="v37-validated-ui"' not in s:s=s.replace('</head>',css+'\n</head>')
if 'id="v37-validated-js"' not in s:s=s.replace('</body>',js+'\n</body>')
p.write_text(s,encoding='utf-8')
print('V37 UI patch applied')
