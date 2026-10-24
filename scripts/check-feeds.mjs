// Vérifie les adresses candidates de flux RSS et affiche un tableau (exécuté par GitHub Actions).
import { readFile, writeFile } from "node:fs/promises";
const list = JSON.parse(await readFile("scripts/feed-candidates.json", "utf8"));
const out = [];
for (const s of list) {
  let res = { name: s.name, zone: s.zone, ok: false };
  for (const url of s.urls) {
    try {
      const r = await fetch(url, { headers: { "User-Agent": "Mozilla/5.0 (Kiosque RSS reader)" }, signal: AbortSignal.timeout(15000) });
      const t = await r.text();
      const items = (t.match(/<(item|entry)[\s>]/g) || []).length;
      if (r.ok && /<(rss|feed|rdf)/i.test(t) && items) {
        const last = (t.match(/<(?:pubDate|updated|dc:date)>([^<]+)</) || [])[1] || "";
        res = { ...res, ok: true, url, items, last: last.trim() };
        break;
      }
      res.err = (res.err ? res.err + " | " : "") + `${url} → HTTP ${r.status}, ${items} articles`;
    } catch (e) { res.err = (res.err ? res.err + " | " : "") + `${url} → ${e.message}`; }
  }
  out.push(res);
  console.log(res.ok ? "OK  " : "FAIL", "|", s.name, "|", res.url || res.err, "|", res.items || 0, "|", res.last || "");
}
await writeFile("feed-check.json", JSON.stringify(out, null, 1));
