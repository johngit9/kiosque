# Kiosque

La presse française en flux RSS : Les Echos, Le Monde, Le Figaro, La Tribune et d'autres, avec des rubriques À la une, High-tech & télécoms et Médias. Chaque article renvoie vers le site du journal.

- **Site web :** https://johngit9.github.io/kiosque. Les flux sont récupérés toutes les 15 minutes par une tâche GitHub (`.github/workflows/pages.yml`).
- **Appli Android :** dernière version, la v46 (1.46) : https://github.com/johngit9/kiosque/releases/latest/download/kiosque.apk (publiée depuis le dossier `apk/` par `.github/workflows/release.yml`). Elle s'installe par-dessus les APK livrés depuis le 25/10/2026 (même clé de signature). Le module natif de capture et de diagnostic (PageShot) est dans `android-plugin/`.
- **Sur votre ordinateur :** `python3 kiosque.py`, puis ouvrez http://localhost:8765.

L'application elle-même tient dans `www/index.html` (et la tâche de fond des notifications dans `www/runners/check.js`). Les sources par défaut sont dans `DEFAULT_FEEDS`.
