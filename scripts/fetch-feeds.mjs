// Récupère côté serveur chaque flux listé dans DEFAULT_FEEDS (www/index.html)
// et en dépose une copie dans <dossier>/feeds/, avec un index.json.
import { readFile, writeFile, mkdir } from "node:fs/promises";
import { join } from "node:path";

const outDir = join(process.argv[2] || "www", "feeds");
const html = await readFile("www/index.html", "utf8");
const block = html.slice(html.indexOf("const DEFAULT_FEEDS"), html.indexOf("const RUBRICS"));
const feeds = [...block.matchAll(/\{ id: "([^"]+)",[^\n]*? url: "([^"]+)"/g)].map(m => ({ id: m[1], url: m[2] }));

await mkdir(outDir, { recursive: true });
const index = {};
await Promise.all(feeds.map(async ({ id, url }) => {
  try {
    const r = await fetch(url, {
      headers: { "User-Agent": "Mozilla/5.0 (Kiosque RSS reader)", Accept: "application/rss+xml, application/xml, text/xml, */*" },
      signal: AbortSignal.timeout(20000),
    });
    if (!r.ok) throw new Error("HTTP " + r.status);
    const txt = await r.text();
    if (!/<(rss|feed|rdf)/i.test(txt)) throw new Error("pas un flux");
    await writeFile(join(outDir, id + ".xml"), txt);
    index[id] = { url, at: new Date().toISOString() };
    console.log("ok  ", id);
  } catch (e) {
    console.log("FAIL", id, url, e.message);
  }
}));
await writeFile(join(outDir, "index.json"), JSON.stringify(index, null, 1));
console.log(Object.keys(index).length + "/" + feeds.length + " flux récupérés");
