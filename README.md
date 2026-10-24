# Kiosque

La presse française en flux RSS : Les Echos, Le Monde, Le Figaro, La Tribune et d'autres, avec des rubriques À la une, High-tech & télécoms et Médias. Chaque article renvoie vers le site du journal.

- **Site web :** https://johngit9.github.io/kiosque. Les flux sont récupérés toutes les 15 minutes par une tâche GitHub (`.github/workflows/pages.yml`).
- **Appli Android :** l'APK est construit à chaque modification (`.github/workflows/android.yml`) et publié dans les Releases : https://github.com/johngit9/kiosque/releases/latest/download/kiosque.apk
- **Sur votre ordinateur :** `python3 kiosque.py`, puis ouvrez http://localhost:8765.

L'application elle-même tient dans `www/index.html`. Les sources par défaut sont dans `DEFAULT_FEEDS`.
