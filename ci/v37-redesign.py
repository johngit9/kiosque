from pathlib import Path

p = Path('www/index.html')
s = p.read_text(encoding='utf-8')

MARK = '/* KIOSQUE V37 REDESIGN */'
if MARK in s:
    raise SystemExit('V37 redesign already applied')

css = r'''
<style>
/* KIOSQUE V37 REDESIGN */
:root{--v37navy:#13233d;--v37navy2:#0d1b31;--v37turq:#19a7a0;--v37turqsoft:#e7f7f5;--v37paper:#f5f7fa;--v37shadow:0 10px 30px rgba(19,35,61,.08)}
body{background:var(--v37paper);padding-bottom:84px}
.sticky{box-shadow:0 1px 0 rgba(19,35,61,.08);background:rgba(255,255,255,.97);backdrop-filter:blur(14px)}
header.top{min-height:72px;border:0;gap:12px;flex-wrap:nowrap;max-width:1380px;margin:auto}
.brand{font-family:var(--f-body);font-size:22px;font-weight:800;color:var(--v37navy)}
.brand svg{width:40px;height:40px}.searchwrap{max-width:620px}.searchwrap .search{height:44px;background:#f0f3f6;border-color:transparent;padding-left:40px}
.searchwrap .search:focus{background:#fff;border-color:var(--v37turq);box-shadow:0 0 0 3px rgba(25,167,160,.12)}
.round{border:0;background:#f0f3f6}.round:hover{background:var(--v37turqsoft);color:var(--v37turq)}
.v37-btn{background:var(--v37navy)!important;color:#fff!important;border:0!important;padding:10px 15px!important}.v37-btn:hover{background:var(--v37turq)!important}
.menuwrap{border:0;background:#fff}.menubar{max-width:1380px;margin:auto;padding-top:4px;padding-bottom:11px}.menubar button{background:#f0f3f6;border:0!important;padding:8px 14px;font-size:13px}.menubar button[aria-current="true"]{background:var(--v37navy);color:#fff}.menubar button.tel[aria-current="true"]{background:var(--v37turq)}
.layout{max-width:1380px;gap:22px;padding-top:20px}.layout main{min-width:0}.viewtitle{font-family:var(--f-body);font-size:25px;font-weight:800;color:var(--v37navy);letter-spacing:-.02em}
nav.sources{background:#fff;border:1px solid #e6ebf0;border-radius:18px;padding:14px 8px;box-shadow:var(--v37shadow)}.navlabel{color:#8290a0;font-weight:700}.src{border-radius:10px}.src[aria-current="true"]{background:var(--v37turqsoft);color:#087c77}
.card,.lead{border:1px solid #e7ebef!important;border-radius:18px!important;background:#fff!important;box-shadow:var(--v37shadow)!important;overflow:hidden}.card:hover{transform:translateY(-1px);box-shadow:0 14px 34px rgba(19,35,61,.12)!important}.grid{gap:14px!important}.card h2,.card h3,.lead h2,.lead h3{color:var(--v37navy);letter-spacing:-.015em}.meta,.note,.status{color:#748191}
#v37dlg{border:0!important;border-radius:24px!important;box-shadow:0 28px 90px rgba(4,15,30,.28)!important}.v37-head{background:#fff}.v37-hero{background:linear-gradient(135deg,var(--v37navy2),#164f58)!important;border-radius:18px!important}.v37-chip{background:var(--v37turqsoft)!important;border:0!important;color:#087c77!important;font-weight:700}
.v37-home{margin:0 0 18px;padding:20px;border-radius:22px;background:linear-gradient(135deg,var(--v37navy2),#164f58);color:#fff;box-shadow:0 16px 38px rgba(19,35,61,.18);display:grid;grid-template-columns:1fr auto;gap:18px;align-items:center}.v37-home-kicker{font-size:11px;text-transform:uppercase;letter-spacing:.12em;font-weight:800;color:#7ee2dc}.v37-home h2{font-family:var(--f-body);font-size:22px;line-height:1.15;margin:5px 0 7px}.v37-home p{margin:0;color:#d7e8eb;font-size:13px}.v37-home button{border:0;border-radius:999px;padding:10px 14px;background:#fff;color:var(--v37navy);font-weight:800;white-space:nowrap}
.v37-bottom{position:fixed;z-index:40;left:50%;bottom:max(10px,env(safe-area-inset-bottom));transform:translateX(-50%);width:min(560px,calc(100% - 22px));height:66px;padding:6px 8px;display:grid;grid-template-columns:repeat(4,1fr);background:rgba(13,27,49,.97);border:1px solid rgba(255,255,255,.1);border-radius:22px;box-shadow:0 18px 45px rgba(5,15,30,.28);backdrop-filter:blur(16px)}.v37-bottom button{border:0;background:transparent;color:#aeb9c7;border-radius:16px;display:grid;place-items:center;align-content:center;gap:2px;font-size:10px;font-weight:700}.v37-bottom button svg{width:20px;height:20px}.v37-bottom button.active,.v37-bottom button:hover{background:rgba(25,167,160,.18);color:#7ee2dc}
@media(max-width:760px){body{padding-bottom:92px}.sticky{position:sticky}.top{padding:8px 12px!important;display:grid!important;grid-template-columns:auto 1fr auto!important;gap:8px!important}.brand{font-size:0}.brand svg{width:38px;height:38px}.searchwrap{grid-column:2;grid-row:1}.searchwrap .search{height:40px;font-size:13px}.tools{grid-column:3;grid-row:1;gap:5px}.tools .stamp,#refresh,#manage{display:none}.v37-btn{font-size:0!important;width:40px;height:40px;padding:0!important;display:grid;place-items:center}.v37-btn:after{content:'◆';font-size:15px;color:#7ee2dc}.menuwrap{grid-column:1/-1}.menubar{padding-left:12px;padding-right:24px}.menubar button{padding:7px 12px;font-size:12px}.layout{display:block!important;padding:14px 12px 20px!important}.layout nav.sources{display:none!important}.viewtitle{font-size:22px}.v37-home{padding:17px;border-radius:18px;grid-template-columns:1fr}.v37-home button{justify-self:start}.grid{grid-template-columns:1fr!important}.card,.lead{border-radius:16px!important}.card{margin:0!important}.v37-bottom{bottom:max(8px,env(safe-area-inset-bottom));height:64px}.v37-bottom button{font-size:9px}}
@media(prefers-color-scheme:dark){:root:not([data-theme="light"]){--v37paper:#0c131c}.sticky,.menuwrap{background:#111b27}.brand,.viewtitle,.card h2,.card h3,.lead h2,.lead h3{color:#edf4fa}.searchwrap .search,.round,.menubar button{background:#182534}.card,.lead,nav.sources{background:#121d29!important;border-color:#223142!important}.v37-home{box-shadow:none}}
</style>
'''

nav = r'''
<nav class="v37-bottom" id="v37bottom" aria-label="Navigation principale V37">
  <button type="button" data-v37nav="home" class="active" aria-label="Accueil"><svg viewBox="0 0 24 24"><path d="M3 11.5 12 4l9 7.5V21h-6v-6H9v6H3z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg><span>Accueil</span></button>
  <button type="button" data-v37nav="watch" aria-label="Mes suivis"><svg viewBox="0 0 24 24"><path d="M12 3v18M3 12h18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg><span>Mes suivis</span></button>
  <button type="button" data-v37nav="explore" aria-label="Explorer"><svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" stroke-width="2"/><path d="m16 16 5 5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg><span>Explorer</span></button>
  <button type="button" data-v37nav="library" aria-label="Bibliothèque"><svg viewBox="0 0 24 24"><path d="M5 4h14v17l-7-4-7 4z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/></svg><span>Bibliothèque</span></button>
</nav>
<script>
(() => {
  const q = document.getElementById('q');
  const openWatch = () => document.getElementById('v37open')?.click();
  const main = document.querySelector('.layout main');
  if (main && !document.getElementById('v37home')) {
    const hero=document.createElement('section'); hero.id='v37home'; hero.className='v37-home';
    hero.innerHTML='<div><div class="v37-home-kicker">Votre briefing</div><h2>L’essentiel de votre actualité, au même endroit.</h2><p>Suivez vos sujets, retrouvez les titres importants et personnalisez votre veille.</p></div><button type="button" id="v37homeopen">Voir mon briefing</button>';
    main.prepend(hero); document.getElementById('v37homeopen').addEventListener('click',openWatch);
  }
  const buttons=[...document.querySelectorAll('[data-v37nav]')];
  const active=b=>{buttons.forEach(x=>x.classList.toggle('active',x===b));};
  buttons.forEach(b=>b.addEventListener('click',()=>{
    active(b); const n=b.dataset.v37nav;
    if(n==='home'){ q && (q.value=''); q?.dispatchEvent(new Event('input',{bubbles:true})); window.scrollTo({top:0,behavior:'smooth'}); }
    if(n==='watch') openWatch();
    if(n==='explore'){ window.scrollTo({top:0,behavior:'smooth'}); setTimeout(()=>q?.focus(),250); }
    if(n==='library'){ document.getElementById('manage')?.click(); setTimeout(()=>{ const t=[...document.querySelectorAll('[role="tab"]')].find(x=>/appli|liens/i.test(x.textContent||'')); t?.click(); },120); }
  }));
})();
</script>
'''

s = s.replace('</body>', css + '\n' + nav + '\n</body>')
p.write_text(s, encoding='utf-8')
print('V37 redesign injected')
