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
  for (const f of config.feeds || []) {
    for (const url of f.urls) {
      try {
        const res = await fetch(url, { headers: { "User-Agent": "Mozilla/5.0 (Kiosque RSS reader)" } });
        if (!res.ok) continue;
        for (const it of parse(await res.text(), f.name)) {
          if (seenSet.has(it.link)) continue;
          seenSet.add(it.link); seen.push(it.link);
          if (first) continue;
          if (it.date && Date.now() - it.date > 24 * 3600 * 1000) continue;
          const text = norm(it.title + " " + it.summary + " " + it.cats + " " + it.link);
          if (ad && ad.test(text)) continue;
          const hit = rules.find(r => r.re.test(text));
          if (hit) fresh.push({ ...it, rule: hit.name });
        }
      } catch (e) { console.log("Kiosque: " + url + " " + e); }
    }
  }
  kvSet("seen", seen.slice(-1500));
  kvSet("initialized", true);
  if (!fresh.length) return 0;

  const h = new Date().getHours();
  if (config.quiet && (h >= 22 || h < 7)) { kvSet("pending", (kvGet("pending", []).concat(fresh)).slice(-30)); return 0; }
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
  return notes.length;
}

addEventListener("checkNews", async (resolve, reject) => {
  try {
    const config = kvGet("config", null);
    if (config && config.enabled) await check(config);
    resolve();
  } catch (e) { console.log("Kiosque: " + e); reject(e); }
});

addEventListener("config", (resolve, reject, args) => {
  try { kvSet("config", args); resolve(); } catch (e) { reject(e); }
});

addEventListener("test", (resolve, reject) => {
  try {
    CapacitorNotifications.schedule([{
      id: 999, title: "◆ Télécoms · Kiosque", body: "Les notifications fonctionnent. Vous serez prévenu des nouveaux articles qui vous intéressent.",
      scheduleAt: new Date(Date.now() + 1000), group: "kiosque",
    }]);
    resolve();
  } catch (e) { reject(e); }
});
