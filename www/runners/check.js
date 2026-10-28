// Tâche de fond Kiosque : toutes les 15 minutes environ, Android réveille ce script,
// qui lit les flux activés et envoie une notification pour les nouveaux articles qui correspondent aux choix de l'utilisateur.
// Réglages reçus de l'appli par l'événement « config » et gardés dans CapacitorKV.

function kvGet(key, fallback) {
  try {
    const r = CapacitorKV.get(key);
    const v = r && typeof r === "object" ? r.value : r;
    return v ? JSON.parse(v) : fallback;
  } catch (e) { return fallback; }
}
function kvSet(key, value) { CapacitorKV.set(key, JSON.stringify(value)); }

function norm(s) {
  s = (s || "").toLowerCase();
  try { s = s.normalize("NFD").replace(/[̀-ͯ]/g, ""); } catch (e) {}
  return s;
}
function decode(s) {
  return (s || "").replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, "$1").replace(/<[^>]+>/g, " ")
    .replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"').replace(/&#0?39;|&apos;/g, "'")
    .replace(/&#(\d+);/g, (_, n) => String.fromCharCode(+n)).replace(/\s+/g, " ").trim();
}
function tag(block, name) {
  const m = block.match(new RegExp("<" + name + "[^>]*>([\\s\\S]*?)</" + name + ">", "i"));
  return m ? decode(m[1]) : "";
}
function parse(xml, source) {
  const out = [];
  const blocks = xml.match(/<item[\s>][\s\S]*?<\/item>/gi) || xml.match(/<entry[\s>][\s\S]*?<\/entry>/gi) || [];
  for (const b of blocks) {
    let link = tag(b, "link");
    if (!link) { const m = b.match(/<link[^>]*href="([^"]+)"/i); link = m ? m[1] : ""; }
    const cats = (b.match(/<category[^>]*>([\s\S]*?)<\/category>/gi) || []).map(decode).join(" ");
    out.push({
      source, link, title: tag(b, "title"),
      summary: tag(b, "description") || tag(b, "summary"),
      date: Date.parse(tag(b, "pubDate") || tag(b, "updated") || tag(b, "published") || tag(b, "dc:date")) || 0,
      cats,
    });
  }
  return out.filter(i => i.title && /^https?:/.test(i.link));
}

async function check(config) {
  const seen = kvGet("seen", []);
  const seenSet = new Set(seen);
  const first = !kvGet("initialized", false);
  const rules = (config.rules || []).map(r => ({ name: r.name, re: new RegExp(r.re) }));
  const ad = config.adRe ? new RegExp(config.adRe) : null;
  const fresh = [];
  const st = { at: Date.now(), feeds: 0, errors: 0, items: 0, fresh: 0, sent: 0, first, rules: rules.length };
  for (const f of config.feeds || []) {
    for (const url of f.urls) {
      try {
        const res = await fetch(url, { headers: { "User-Agent": "Mozilla/5.0 (Kiosque RSS reader)" } });
        if (!res.ok) { st.errors++; continue; }
        st.feeds++;
        for (const it of parse(await res.text(), f.name)) {
          st.items++;
          if (seenSet.has(it.link)) continue;
          seenSet.add(it.link); seen.push(it.link);
          if (first) continue;
          if (it.date && Date.now() - it.date > 24 * 3600 * 1000) continue;
          const text = norm(it.title + " " + it.summary + " " + it.cats + " " + it.link);
          if (ad && ad.test(text)) continue;
          const hit = rules.find(r => r.re.test(text));
          if (hit) fresh.push({ ...it, rule: hit.name });
        }
      } catch (e) { st.errors++; console.log("Kiosque: " + url + " " + e); }
    }
  }
  kvSet("seen", seen.slice(-1500));
  kvSet("initialized", true);
  st.fresh = fresh.length;
  kvSet("status", st);
  if (!fresh.length) return 0;

  const h = new Date().getHours();
  if (config.quiet && (h >= 22 || h < 7)) { st.waiting = true; kvSet("status", st); kvSet("pending", (kvGet("pending", []).concat(fresh)).slice(-30)); return 0; }
  const all = kvGet("pending", []).concat(fresh);
  kvSet("pending", []);

  let id = kvGet("nextId", 1000);
  const notes = [];
  if (all.length <= 4) {
    for (const it of all) {
      notes.push({ id: id++, title: "◆ " + it.rule + " · " + it.source, body: it.title, scheduleAt: new Date(Date.now() + 1500), group: "kiosque" });
    }
  } else {
    const byRule = {};
    all.forEach(it => { byRule[it.rule] = (byRule[it.rule] || 0) + 1; });
    const detail = Object.keys(byRule).map(k => byRule[k] + " " + k).join(" · ");
    notes.push({
      id: id++, title: "Kiosque · " + all.length + " nouveaux articles",
      body: detail + "\n" + all.slice(0, 4).map(it => "• " + it.title + " (" + it.source + ")").join("\n"),
      scheduleAt: new Date(Date.now() + 1500), group: "kiosque",
    });
  }
  kvSet("nextId", id > 900000 ? 1000 : id);
  CapacitorNotifications.schedule(notes);
  st.sent = notes.length; kvSet("status", st);
  return notes.length;
}

// ——— Matchs : notification la veille (vers 18 h) et le jour même (vers 9 h) ———
// Équipes suivies (choisies dans l'appli) et, si demandé, clubs français en coupe d'Europe. Calendriers fixturedownload.com.
const FR_RE = /\b(paris|psg|marseille|lyon|monaco|lille|lens|rennes|nice|brest|strasbourg|nantes|toulouse|auxerre|angers|havre|lorient|metz|troyes|le mans|reims|montpellier|saint-etienne)\b/;
function pad2(n) { return (n < 10 ? "0" : "") + n; }
function ymd(d) { return d.getFullYear() + pad2(d.getMonth() + 1) + pad2(d.getDate()); }
function isoTime(s) {
  const m = /^(\d{4})-(\d\d)-(\d\d)[T ](\d\d):(\d\d)(?::(\d\d))?/.exec(s || "");
  return m ? Date.UTC(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +(m[6] || 0)) : NaN;
}
// Même règle que dans l'appli : « Olympique de Marseille » et « Marseille » sont la même équipe.
const CANON = { "olympique de marseille": "marseille", "paris saint-germain": "paris saint-germain", "paris": "paris saint-germain", "psg": "paris saint-germain", "olympique lyonnais": "lyon", "losc lille": "lille", "rc lens": "lens", "stade rennais fc": "rennes", "stade rennais": "rennes", "as monaco": "monaco", "ogc nice": "nice", "stade brestois 29": "brest", "rc strasbourg alsace": "strasbourg", "fc nantes": "nantes", "toulouse fc": "toulouse", "aj auxerre": "auxerre", "angers sco": "angers", "havre athletic club": "le havre", "le havre ac": "le havre", "fc lorient": "lorient", "fc metz": "metz", "estac troyes": "troyes" };
function teamKey(n) { const k = norm(n).replace(/\s+/g, " ").trim(); return CANON[k] || k; }
function short(n) { const k = teamKey(n); return k === "marseille" ? "OM" : k === "paris saint-germain" ? "PSG" : n; }
async function fetchMatches(m) {
  const now = new Date(), out = [], teams = {};
  (m.teams || []).forEach(t => { teams[t] = 1; });
  for (const lg of m.comps || []) {
    try {
      const res = await fetch(lg.url);
      if (!res.ok) continue;
      const list = JSON.parse(await res.text());
      for (const r of list) {
        const t = isoTime(r.DateUtc);
        if (!(t > now.getTime() - 3600e3 && t < now.getTime() + 3 * 864e5)) continue;
        const mine = teams[teamKey(r.HomeTeam || "")] || teams[teamKey(r.AwayTeam || "")];
        const fr = lg.euro && m.euroFr && FR_RE.test(norm((r.HomeTeam || "") + " " + (r.AwayTeam || "")));
        if (!mine && !fr) continue;
        out.push({ id: lg.id + ":" + r.MatchNumber, t, lg: lg.name, icon: lg.icon || "⚽", title: short(r.HomeTeam) + " – " + short(r.AwayTeam) });
      }
    } catch (e) { console.log("Kiosque sport: " + lg.id + " " + e); }
  }
  return out;
}
async function checkMatches(m) {
  if (!m || !(m.teams && m.teams.length || m.euroFr) || !(m.comps && m.comps.length)) return;
  const now = new Date(), h = now.getHours();
  if (h < 9 || h >= 23) return;
  // Calendrier relu au plus toutes les heures, gardé entre deux réveils.
  const key = JSON.stringify([m.teams, m.euroFr, (m.comps || []).map(c => c.id)]);
  let cache = kvGet("matches3", null);
  if (!cache || now.getTime() - cache.at > 3600e3 || cache.key !== key) {
    cache = { at: now.getTime(), key, list: await fetchMatches(m) };
    kvSet("matches3", cache);
  }
  const done = kvGet("matchDone", []);
  const today = ymd(now), tomorrow = ymd(new Date(now.getTime() + 864e5));
  let id = kvGet("nextId", 1000);
  const notes = [];
  for (const x of cache.list) {
    if (!(x.t > now.getTime())) continue;
    const k = new Date(x.t), day = ymd(k), time = pad2(k.getHours()) + ":" + pad2(k.getMinutes());
    if (day === tomorrow && h >= 18 && done.indexOf("v" + x.id) < 0) {
      done.push("v" + x.id);
      notes.push({ id: id++, title: x.icon + " Demain " + time + " · " + x.title, body: x.lg + " · c'est demain.", scheduleAt: new Date(Date.now() + 1500), group: "kiosque-foot" });
    }
    if (day === today && done.indexOf("j" + x.id) < 0) {
      done.push("j" + x.id);
      notes.push({ id: id++, title: x.icon + " Aujourd'hui " + time + " · " + x.title, body: x.lg + " · c'est aujourd'hui.", scheduleAt: new Date(Date.now() + 1500), group: "kiosque-foot" });
    }
  }
  kvSet("matchDone", done.slice(-300));
  kvSet("nextId", id > 900000 ? 1000 : id);
  if (notes.length) CapacitorNotifications.schedule(notes);
  const st = kvGet("status", {}); st.matches = notes.length; st.matchAt = Date.now(); kvSet("status", st);
}

async function runAll() {
  const config = kvGet("config", null);
  if (config && config.enabled) await check(config);
  try { if (config) await checkMatches(config.matches); } catch (e) { console.log("Kiosque sport: " + e); }
  const st = kvGet("status", {});
  st.ran = Date.now(); st.enabled = !!(config && config.enabled); st.hasConfig = !!config;
  kvSet("status", st);
  return st;
}

addEventListener("checkNews", async (resolve, reject) => {
  try { const st = kvGet("status", {}); st.wake = Date.now(); kvSet("status", st); await runAll(); resolve(); }
  catch (e) { console.log("Kiosque: " + e); reject(e); }
});

// Bouton « Vérifier maintenant » de l'appli : même vérification, résultat renvoyé à l'appli.
addEventListener("checkNow", async (resolve, reject) => {
  try { resolve(await runAll()); } catch (e) { reject(e); }
});

// Dernière vérification faite en arrière-plan (heure, journaux lus, notifications envoyées).
addEventListener("status", (resolve, reject) => {
  try { resolve(kvGet("status", {})); } catch (e) { reject(e); }
});

addEventListener("config", (resolve, reject, args) => {
  try { kvSet("config", args); resolve(); } catch (e) { reject(e); }
});

addEventListener("test", (resolve, reject) => {
  try {
    CapacitorNotifications.schedule([{
      id: 999, title: "◆ Kiosque", body: "Les notifications fonctionnent. Vous serez prévenu des nouveaux articles qui vous intéressent.",
      scheduleAt: new Date(Date.now() + 1000), group: "kiosque",
    }]);
    resolve();
  } catch (e) { reject(e); }
});
