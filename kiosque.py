#!/usr/bin/env python3
"""Kiosque : petit serveur local qui affiche index.html et récupère les flux RSS
lui-même, sans passer par des relais publics.

Lancement :  python3 kiosque.py      puis ouvrir http://localhost:8765
"""
import http.server
import os
import socketserver
import urllib.parse
import urllib.request
import webbrowser

PORT = int(os.environ.get("PORT", "8765"))
HERE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "www")
UA = "Mozilla/5.0 (Kiosque RSS reader)"


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=HERE, **kwargs)

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path != "/api/feed":
            return super().do_GET()
        url = urllib.parse.parse_qs(parsed.query).get("url", [""])[0]
        if not url.startswith(("http://", "https://")):
            return self.send_error(400, "url manquante")
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/rss+xml, application/xml, text/xml, */*"})
            with urllib.request.urlopen(req, timeout=15) as r:
                body = r.read(5_000_000)
        except Exception as e:  # flux injoignable : le navigateur essaiera les relais
            return self.send_error(502, str(e)[:200])
        self.send_response(200)
        self.send_header("Content-Type", "application/xml; charset=utf-8")
        self.send_header("Cache-Control", "max-age=120")
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    socketserver.ThreadingTCPServer.allow_reuse_address = True
    with socketserver.ThreadingTCPServer(("127.0.0.1", PORT), Handler) as httpd:
        print(f"Kiosque tourne sur http://localhost:{PORT}  (Ctrl+C pour arrêter)")
        try:
            webbrowser.open(f"http://localhost:{PORT}")
        except Exception:
            pass
        httpd.serve_forever()
