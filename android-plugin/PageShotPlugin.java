package fr.kiosque.rss;

import android.app.Activity;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.TextView;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.FileOutputStream;

// Capture de page faite par le téléphone, en version ordinateur (page de 1366 px CSS réduite à la largeur de l'écran).
// La page s'affiche par-dessus l'appli pendant la capture : elle défile jusqu'en bas pour tout charger,
// puis chaque écran est copié (pixels réels affichés) et recollé à sa place dans l'image de la page entière.
// Les régies publicitaires sont bloquées et les connexions de l'utilisateur aux journaux sont utilisées.
@CapacitorPlugin(name = "PageShot")
public class PageShotPlugin extends Plugin {
    private static final String DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    // Régies publicitaires bloquées pendant la capture : leurs requêtes reçoivent une réponse vide.
    private static final String[] AD_HOSTS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "googletagservices.com", "adservice.google.",
        "amazon-adsystem.com", "adnxs.com", "criteo.com", "criteo.net", "teads.tv", "smartadserver.com", "sascdn.com",
        "taboola.com", "outbrain.com", "rubiconproject.com", "pubmatic.com", "openx.net", "casalemedia.com", "adsrvr.org",
        "moatads.com", "adsafeprotected.com", "doubleverify.com", "seedtag.com", "invibes.com", "ogury.", "presage.io",
        "richaudience.com", "adform.net", "3lift.com", "sharethrough.com", "sublime.xyz", "ayads.co", "adotmob.com",
        "quantum-advertising.com", "yieldlove", "smilewanted.com", "onetag-sys.com", "id5-sync.com", "teads.com",
        "ligatus.com", "plista.com", "mgid.com", "outbrainimg.com", "taboolasyndication.com", "adhese.com", "improvedigital.com",
        "eskimi.com", "missena.io", "nexx360.io", "weborama.", "adsco.re", "hubvisor.io", "optidigital.com", "freewheel.tv",
        "spotxchange.com", "springserve.com", "dailymotion.com/ads", "imasdk.googleapis.com", "pagead2.", "securepubads."
    };

    static boolean isAd(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        int s = u.indexOf("://");
        if (s >= 0) u = u.substring(s + 3);
        for (String h : AD_HOSTS) if (u.contains(h)) return true;
        return false;
    }

    @PluginMethod()
    public void capture(final PluginCall call) {
        final String url = call.getString("url");
        if (url == null || url.length() == 0) { call.reject("adresse manquante"); return; }
        final String css = call.getString("css", "");
        final String js = call.getString("js", "");
        final int cssWidth = call.getInt("width", 1366);
        final int maxCssHeight = call.getInt("maxHeight", 16000);
        final int wait = call.getInt("wait", 3000);
        final boolean manual = call.getBoolean("manual", false);
        final boolean landscape = call.getBoolean("landscape", false);
        final Activity act = getActivity();
        new Handler(Looper.getMainLooper()).post(new Runnable() { public void run() {
            Job j = new Job(act, call, url, css, js, cssWidth, maxCssHeight, wait);
            j.manual = manual;
            j.landscape = manual && landscape;
            j.start();
        }});
    }

    // Économie de batterie d'Android : si elle s'applique à Kiosque, la tâche de fond des notifications peut être bloquée.
    @PluginMethod()
    public void battery(PluginCall call) {
        JSObject r = new JSObject();
        boolean ign = true;
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                android.os.PowerManager pm = (android.os.PowerManager) getContext().getSystemService(android.content.Context.POWER_SERVICE);
                ign = pm.isIgnoringBatteryOptimizations(getContext().getPackageName());
            }
        } catch (Throwable e) {}
        r.put("ignoring", ign);
        call.resolve(r);
    }

    // Ouvre un écran des réglages Android : « battery » (économie de batterie), « notif » (notifications de l'appli), sinon la fiche de l'appli.
    @PluginMethod()
    public void openSettings(PluginCall call) {
        String what = call.getString("what", "app");
        String pkg = getContext().getPackageName();
        android.content.Intent i;
        if ("battery".equals(what) && Build.VERSION.SDK_INT >= 23) i = new android.content.Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS");
        else if ("notif".equals(what) && Build.VERSION.SDK_INT >= 26) { i = new android.content.Intent("android.settings.APP_NOTIFICATION_SETTINGS"); i.putExtra("android.provider.extra.APP_PACKAGE", pkg); }
        else i = new android.content.Intent("android.settings.APPLICATION_DETAILS_SETTINGS", android.net.Uri.parse("package:" + pkg));
        i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        try { getContext().startActivity(i); call.resolve(); }
        catch (Throwable e) {
            try {
                android.content.Intent j = new android.content.Intent("android.settings.APPLICATION_DETAILS_SETTINGS", android.net.Uri.parse("package:" + pkg));
                j.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(j); call.resolve();
            } catch (Throwable e2) { call.reject(e2.toString()); }
        }
    }

    // Téléchargement d'une image (services de capture) dans le cache de l'appli : pas de blocage CORS,
    // l'image s'affiche et se partage directement.
    @PluginMethod()
    public void download(final PluginCall call) {
        final String url = call.getString("url");
        if (url == null || url.length() == 0) { call.reject("adresse manquante"); return; }
        final int timeout = call.getInt("timeout", 90000);
        // Type de fichier accepté (début du type MIME) : « image/ » par défaut, « » pour tout (polices, feuilles de style).
        final String accept = call.getString("accept", "image/");
        final File dir = getContext().getCacheDir();
        new Thread(new Runnable() { public void run() {
            HttpURLConnection c = null;
            try {
                c = open(url, timeout);
                c.setInstanceFollowRedirects(true);
                // Envoi d'une page à un service de capture (POST JSON) au lieu d'une simple lecture.
                String body = call.getString("body");
                if (body != null) {
                    c.setRequestMethod("POST");
                    c.setDoOutput(true);
                    c.setRequestProperty("Content-Type", call.getString("contentType", "application/json"));
                    java.io.OutputStream out = c.getOutputStream();
                    out.write(body.getBytes("UTF-8")); out.close();
                }
                int code = c.getResponseCode();
                String type = c.getContentType() == null ? "" : c.getContentType().toLowerCase();
                InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                if (code >= 400 || !type.startsWith(accept)) {
                    StringBuilder sb = new StringBuilder();
                    if (in != null) { byte[] b = new byte[2048]; int n = in.read(b); if (n > 0) sb.append(new String(b, 0, n, "UTF-8")); in.close(); }
                    call.reject("HTTP " + code + " " + sb.toString().replaceAll("\\s+", " ").trim());
                    return;
                }
                String path = new URL(url).getPath().toLowerCase();
                String ext = type.contains("png") ? ".png" : type.contains("webp") ? ".webp" : type.startsWith("image/") ? ".jpg"
                    : path.endsWith(".woff2") || type.contains("woff2") ? ".woff2" : path.endsWith(".woff") || type.contains("woff") ? ".woff"
                    : path.endsWith(".ttf") ? ".ttf" : path.endsWith(".otf") ? ".otf" : ".bin";
                File f = new File(dir, "kiosque-shot-" + System.currentTimeMillis() + ext);
                FileOutputStream os = new FileOutputStream(f);
                byte[] buf = new byte[65536]; int n; long total = 0;
                while ((n = in.read(buf)) > 0) { os.write(buf, 0, n); total += n; }
                os.close(); in.close();
                if (total < (accept.length() == 0 ? 100 : 1000)) { f.delete(); call.reject("fichier vide"); return; }
                File[] old = dir.listFiles();
                if (old != null) for (File o : old)
                    if (o.getName().startsWith("kiosque-shot-") && !o.equals(f) && System.currentTimeMillis() - o.lastModified() > 3600000) o.delete();
                JSObject r = new JSObject();
                r.put("path", f.getAbsolutePath());
                r.put("type", type);
                // Contenu en « data: » sur demande : utilisable tel quel dans l'image finale (texte dessiné par le navigateur).
                if (Boolean.TRUE.equals(call.getBoolean("data", Boolean.FALSE)) && total < 8000000) {
                    byte[] all = new byte[(int) total];
                    java.io.FileInputStream fi = new java.io.FileInputStream(f);
                    int off = 0; while (off < all.length && (n = fi.read(all, off, all.length - off)) > 0) off += n;
                    fi.close();
                    String mime = type.split(";")[0].trim();
                    if (mime.length() == 0 || mime.startsWith("application/") || mime.startsWith("binary/") || mime.startsWith("text/"))
                        mime = ext.equals(".woff2") ? "font/woff2" : ext.equals(".woff") ? "font/woff" : ext.equals(".ttf") ? "font/ttf" : ext.equals(".otf") ? "font/otf" : mime;
                    r.put("data", "data:" + mime + ";base64," + android.util.Base64.encodeToString(all, android.util.Base64.NO_WRAP));
                }
                call.resolve(r);
            } catch (Throwable e) {
                call.reject(e.toString());
            } finally { if (c != null) c.disconnect(); }
        }}).start();
    }

    // Connexion « comme le navigateur de l'appli » : même identité et mêmes cookies (CAPTCHA déjà validés dans le lecteur).
    HttpURLConnection open(String url, int timeout) throws java.io.IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(timeout);
        try { c.setRequestProperty("User-Agent", WebSettings.getDefaultUserAgent(getContext())); } catch (Throwable e) {}
        c.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8");
        try { String ck = CookieManager.getInstance().getCookie(url); if (ck != null) c.setRequestProperty("Cookie", ck); } catch (Throwable e) {}
        return c;
    }

    // Suit les redirections d'une adresse (par ex. archive.ph/newest/… vers la copie archivée) et renvoie l'adresse finale.
    @PluginMethod()
    public void resolve(final PluginCall call) {
        final String url = call.getString("url");
        if (url == null || url.length() == 0) { call.reject("adresse manquante"); return; }
        new Thread(new Runnable() { public void run() {
            HttpURLConnection c = null;
            try {
                String u = url; int code = 0;
                for (int i = 0; i < 8; i++) {
                    c = open(u, 30000);
                    c.setInstanceFollowRedirects(false);
                    code = c.getResponseCode();
                    String loc = c.getHeaderField("Location");
                    if (code >= 300 && code < 400 && loc != null) { u = new URL(new URL(u), loc).toString(); c.disconnect(); c = null; continue; }
                    break;
                }
                String head = "";
                try {
                    InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    if (in != null) { byte[] b = new byte[8192]; int n = in.read(b); if (n > 0) head = new String(b, 0, n, "UTF-8"); in.close(); }
                } catch (Throwable e) {}
                JSObject r = new JSObject();
                r.put("url", u);
                r.put("status", code);
                r.put("head", head);
                call.resolve(r);
            } catch (Throwable e) {
                call.reject(e.toString());
            } finally { if (c != null) c.disconnect(); }
        }}).start();
    }

    private class Job {
        final Activity act; final PluginCall call; final String url, css, js;
        final int cssWidth, maxCssHeight, wait;
        final Handler h = new Handler(Looper.getMainLooper());
        final float density;
        WebView wv; FrameLayout root, overlay; TextView label; boolean started, done;
        Bitmap out, strip; Canvas outCanvas; int outH, viewH, viewW;

        Job(Activity act, PluginCall call, String url, String css, String js, int cssWidth, int maxCssHeight, int wait) {
            this.act = act; this.call = call; this.url = url; this.css = css; this.js = js;
            this.cssWidth = cssWidth; this.maxCssHeight = maxCssHeight; this.wait = wait;
            this.density = act.getResources().getDisplayMetrics().density;
        }

        int dp(int v) { return Math.round(v * density); }
        boolean manual, landscape;
        int prevOrient = -99;

        void start() {
            try {
                // Copie en mode paysage : l'écran est plus large, la page ordinateur est dessinée avec plus de pixels (texte plus net).
                if (landscape) { prevOrient = act.getRequestedOrientation(); act.setRequestedOrientation(6 /* SCREEN_ORIENTATION_SENSOR_LANDSCAPE */); }
                root = (FrameLayout) act.findViewById(android.R.id.content);
                overlay = new FrameLayout(act);
                overlay.setBackgroundColor(Color.rgb(0x13, 0x23, 0x3d));
                overlay.setClickable(true);
                label = new TextView(act);
                label.setText("Capture de la page en cours…");
                label.setTextColor(Color.WHITE);
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                label.setGravity(Gravity.CENTER);
                overlay.addView(label, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

                // Page entière dessinable d'un coup (et pas seulement la partie affichée) : à activer avant de créer le WebView.
                if (Build.VERSION.SDK_INT >= 21) try { WebView.enableSlowWholeDocumentDraw(); } catch (Throwable e) {}
                wv = new WebView(act);
                WebSettings s = wv.getSettings();
                s.setJavaScriptEnabled(true);
                s.setDomStorageEnabled(true);
                s.setUserAgentString(DESKTOP_UA);
                s.setUseWideViewPort(true);
                s.setLoadWithOverviewMode(true);
                s.setTextZoom(100);
                s.setSupportZoom(true);
                s.setBuiltInZoomControls(true);
                s.setDisplayZoomControls(false);
                s.setMediaPlaybackRequiresUserGesture(true);
                if (Build.VERSION.SDK_INT >= 21) {
                    s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
                    CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true);
                }
                // Pas de mode sombre forcé sur la page capturée.
                try { s.getClass().getMethod("setForceDark", int.class).invoke(s, 0); } catch (Throwable e) {}
                try { s.getClass().getMethod("setAlgorithmicDarkeningAllowed", boolean.class).invoke(s, false); } catch (Throwable e) {}
                wv.setBackgroundColor(Color.WHITE);
                wv.setVerticalScrollBarEnabled(false);
                wv.setHorizontalScrollBarEnabled(false);
                if (manual) { s.setSupportZoom(false); s.setBuiltInZoomControls(false); }
                else wv.setOnTouchListener(new View.OnTouchListener() { public boolean onTouch(View v, MotionEvent e) { return true; } });
                // WebView plus large que l'écran (1366 px CSS × 1,5) : la page ordinateur est dessinée nette, sans être réduite.
                pxW = Math.round(cssWidth * 1.5f);
                wv.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView v, String u) { viewport(); onLoaded(); }
                    @Override public WebResourceResponse shouldInterceptRequest(WebView v, String u) {
                        if (!manual && isAd(u)) return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                        return null;
                    }
                });
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(manual ? ViewGroup.LayoutParams.MATCH_PARENT : pxW, ViewGroup.LayoutParams.MATCH_PARENT);
                lp.topMargin = dp(56);
                overlay.addView(wv, lp);
                if (manual) addButtons();
                root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                wv.loadUrl(url);
                // Mise en page ordinateur dès que possible pendant le chargement.
                for (int i = 1; i <= 10; i++) h.postDelayed(new Runnable() { public void run() { if (!done) viewport(); } }, i * 400);
                // Si la page ne finit jamais de charger (pubs, vidéos), on capture quand même.
                h.postDelayed(new Runnable() { public void run() { onLoaded(); } }, manual ? 8000 : 20000);
                h.postDelayed(new Runnable() { public void run() { fail("délai dépassé"); } }, manual ? 900000 : 150000);
            } catch (Throwable e) { fail(e.toString()); }
        }

        // Force une page large de 1366 px CSS (version ordinateur), même si le site prévoit une version mobile.
        void viewport() {
            wv.evaluateJavascript("(function(){try{var m=document.querySelector('meta[name=viewport]');"
                + "if(!m){m=document.createElement('meta');m.name='viewport';(document.head||document.documentElement).appendChild(m);}"
                + "var c='width=" + cssWidth + "';if(m.content!==c)m.content=c;}catch(e){}})()", null);
        }

        void inject() {
            String code = "(function(){try{var s=document.getElementById('__kshot');if(!s){s=document.createElement('style');s.id='__kshot';(document.head||document.documentElement).appendChild(s);}"
                + "s.textContent=" + org.json.JSONObject.quote(css) + ";"
                + "document.querySelectorAll('img[loading=lazy]').forEach(function(i){i.loading='eager';});"
                + "document.querySelectorAll('img[data-src]').forEach(function(i){if(!i.getAttribute('src')||i.src.indexOf('data:')==0)i.src=i.getAttribute('data-src');});"
                + "}catch(e){}})();" + js;
            wv.evaluateJavascript(code, null);
        }

        void onLoaded() {
            if (started || done) return;
            started = true;
            if (manual) {
                h.postDelayed(new Runnable() { public void run() {
                    if (done) return;
                    viewport();
                    // Script fourni par l'appli : colonne de droite masquée (sans déplacer la page) et repérage de la fin de l'article.
                    if (js != null && js.length() > 0) wv.evaluateJavascript(js, null);
                    h.postDelayed(new Runnable() { public void run() { manualStart(); } }, 600);
                }}, 900);
                return;
            }
            h.postDelayed(new Runnable() { public void run() {
                if (done) return;
                viewport();
                inject();
                // Premier défilement jusqu'en bas puis retour en haut : charge les images et blocs différés.
                wv.evaluateJavascript("(function(){window.__kscroll=0;var y=0,t=setInterval(function(){y+=500;window.scrollTo(0,y);"
                    + "var b=document.body,d=document.documentElement,H=Math.max(b?b.scrollHeight:0,d.scrollHeight);"
                    + "if(y>=H||y>" + maxCssHeight + "){clearInterval(t);window.scrollTo(0,0);window.__kscroll=1;}},120);})()", null);
                waitScroll(0);
            }}, wait);
        }

        void waitScroll(final int n) {
            h.postDelayed(new Runnable() { public void run() {
                if (done) return;
                wv.evaluateJavascript("window.__kscroll", new ValueCallback<String>() {
                    public void onReceiveValue(String v) {
                        if ("1".equals(v) || n > 60) { inject(); h.postDelayed(new Runnable() { public void run() { begin(); } }, 1500); }
                        else waitScroll(n + 1);
                    }
                });
            }}, 500);
        }

        int pxW; String mode = "strips";

        // Retour en haut, puis la page entière est dessinée d'un coup dans l'image.
        void begin() {
            if (done) return;
            label.setText("Capture de la page en cours… (dessin)");
            wv.scrollTo(0, 0);
            wv.evaluateJavascript("window.scrollTo(0,0)", null);
            h.postDelayed(new Runnable() { public void run() { drawWhole(false); } }, 900);
        }

        @SuppressWarnings("deprecation")
        void drawWhole(final boolean soft) {
            if (done) return;
            try {
                int w = wv.getWidth(), vh = wv.getHeight();
                float scale = wv.getScale();
                int cssH = Math.min(wv.getContentHeight(), maxCssHeight);
                int fullH = Math.max(vh, Math.round(cssH * scale));
                // Limites mémoire d'un téléphone : au-delà, l'image est réduite (jamais coupée).
                float f = Math.min(1f, Math.min(16000f / fullH, (float) Math.sqrt(32e6 / ((double) w * fullH))));
                out = Bitmap.createBitmap(Math.max(1, Math.round(w * f)), Math.max(1, Math.round(fullH * f)), Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(out);
                c.drawColor(Color.WHITE);
                c.scale(f, f);
                c.translate(-wv.getScrollX(), -wv.getScrollY());
                wv.draw(c);
                int oh = out.getHeight();
                boolean empty = uniform(out, 0, oh);
                // Seul le premier écran dessiné : le reste de la page est vide.
                boolean partial = !empty && fullH > vh * 2 && uniform(out, Math.round(vh * f) + 4, oh);
                if (!empty && !partial) { strips = 1; mode = soft ? "page entière (logiciel)" : "page entière"; save(); return; }
                out.recycle(); out = null;
                if (!soft) {
                    wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    h.postDelayed(new Runnable() { public void run() { drawWhole(true); } }, 900);
                    return;
                }
            } catch (Throwable e) { if (out != null) { out.recycle(); out = null; } }
            mode = "écran par écran";
            beginStrips();
        }


        // ---- Copie pendant un défilement automatique, comme au doigt : rien n'est filtré ni masqué. ----
        // L'appli glisse le doigt sur la page, copie l'écran affiché, mesure de combien la page a bougé
        // en comparant avec l'écran précédent, et ajoute seulement la partie nouvelle en bas de l'image.
        android.widget.Button finishBtn;
        Bitmap prevF; int outY, steps; boolean finishing;

        void addButtons() {
            label.setText("Chargement de la page…");
            android.widget.LinearLayout bar = new android.widget.LinearLayout(act);
            bar.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setPadding(dp(8), 0, dp(8), 0);
            android.widget.Button cancel = new android.widget.Button(act);
            cancel.setText("Annuler");
            cancel.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { fail("copie annulée"); } });
            finishBtn = new android.widget.Button(act);
            finishBtn.setText("Terminer");
            finishBtn.setEnabled(false);
            finishBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { manualFinish(); } });
            overlay.removeView(label);
            label.setGravity(Gravity.CENTER_VERTICAL);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            bar.addView(cancel, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));
            bar.addView(label, new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            bar.addView(finishBtn, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));
            overlay.addView(bar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        }

        void manualStart() {
            if (done) return;
            try {
                viewW = wv.getWidth(); viewH = wv.getHeight();
                ensureOut(viewH * 4);
                strip = Bitmap.createBitmap(viewW, viewH, Bitmap.Config.ARGB_8888);
                prevF = Bitmap.createBitmap(viewW, viewH, Bitmap.Config.ARGB_8888);
            } catch (Throwable e) { fail("mémoire insuffisante"); return; }
            label.setText("Copie en cours…");
            finishBtn.setEnabled(true);
            grab(new Runnable() { public void run() {
                if (done || finishing) return;
                if (blankStrip) { fail("le téléphone n'a pas pu copier l'écran"); return; }
                new Canvas(prevF).drawBitmap(strip, 0, 0, null);
                outCanvas.drawBitmap(strip, 0, 0, null);
                outY = viewH;
                checkEnd(new Runnable() { public void run() { nextStep(); } });
            }}, -1);
        }

        // Image de sortie agrandie au fur et à mesure (limite : 30 000 px de haut).
        void ensureOut(int need) {
            // Limite ~50 millions de pixels (≈200 Mo) pour ne pas saturer la mémoire du téléphone.
            int cap = Math.max(viewH, Math.min(need, Math.min(30000, (int) (50000000L / Math.max(1, viewW)))));
            if (out != null && out.getHeight() >= cap) return;
            Bitmap nb = Bitmap.createBitmap(viewW, cap, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(nb);
            c.drawColor(Color.WHITE);
            if (out != null) { c.drawBitmap(out, 0, 0, null); out.recycle(); }
            out = nb; outCanvas = new Canvas(out); outH = cap;
        }

        int stuck;
        void nextStep() { nextStep(false); }
        // js = true : défilement par la page elle-même (window.scrollBy), quand le geste du doigt n'a pas fait bouger la page.
        void nextStep(final boolean js) {
            if (done || finishing) return;
            // Glissement d'un peu plus d'un demi-écran : assez de recouvrement pour recoller juste, même avec un menu collant.
            final int d = Math.round(viewH * (landscape ? 0.55f : 0.7f));
            final Runnable after = new Runnable() { public void run() {
                h.postDelayed(new Runnable() { public void run() {
                    if (done || finishing) return;
                    grab(new Runnable() { public void run() {
                        if (done || finishing) return;
                        steps++;
                        if (blankStrip) { manualFinish(); return; }
                        int sh = shift(prevF, strip, d);
                        if (sh <= 2) {
                            // La page n'a pas bougé : on vérifie qu'il ne reste vraiment rien en dessous avant de s'arrêter.
                            wv.evaluateJavascript("(function(){var e=document.scrollingElement||document.documentElement,b=document.body;"
                                + "var hh=Math.max(e.scrollHeight,b?b.scrollHeight:0);return hh-(window.scrollY+window.innerHeight)})()", new ValueCallback<String>() {
                                public void onReceiveValue(String v) {
                                    if (done || finishing) return;
                                    float rem = 0;
                                    try { rem = Float.parseFloat(v.replace("\"", "")); } catch (Throwable e) {}
                                    if (rem > 40 && stuck < 4 && steps <= 120) { stuck++; nextStep(true); } else manualFinish();
                                }
                            });
                            return;
                        }
                        stuck = 0;
                        try { ensureOut(outY + sh > outH ? Math.max(outH * 3 / 2, outY + sh) : outH); } catch (Throwable e) { manualFinish(); return; }
                        int add = Math.min(Math.min(sh, outH - outY), viewH - fixBot - fixTop);
                        if (add > 0) {
                            // Partie nouvelle : juste au-dessus d'une éventuelle barre fixe du bas.
                            Rect src = new Rect(0, viewH - fixBot - add, viewW, viewH - fixBot), dst = new Rect(0, outY, viewW, outY + add);
                            outCanvas.drawBitmap(strip, src, dst, null);
                            outY += add;
                        }
                        new Canvas(prevF).drawBitmap(strip, 0, 0, null);
                        label.setText("Copie en cours… " + outY + " px");
                        if (outY >= outH && outH >= Math.min(30000, (int) (50000000L / Math.max(1, viewW))) || steps > 120) { manualFinish(); return; }
                        checkEnd(new Runnable() { public void run() { nextStep(); } });
                    }}, -1);
                }}, js ? 700 : 350);
            }};
            if (js) {
                float sc = wv.getScale(); if (sc <= 0) sc = 1;
                int css = Math.max(1, Math.round(d / sc));
                wv.evaluateJavascript("(function(){try{var a=[document.documentElement,document.body];for(var i=0;i<a.length;i++)if(a[i]){a[i].style.setProperty('overflow-y','auto','important');}"
                    + "var L=document.querySelectorAll('body *');for(var j=0;j<L.length;j++){var s=getComputedStyle(L[j]);if(s.position!=='fixed')continue;var r=L[j].getBoundingClientRect();"
                    + "if(r.height>innerHeight*0.45&&r.width>innerWidth*0.5)L[j].style.setProperty('display','none','important');}}catch(e){}"
                    + "window.scrollBy(0," + css + ");})()", null);
                h.post(after);
            } else swipe(d, after);
        }

        // Glissement du doigt vers le haut de d pixels, lent puis maintenu (pas d'élan), comme un utilisateur.
        void swipe(final int d, final Runnable then) {
            final long t0 = android.os.SystemClock.uptimeMillis();
            final float x = viewW / 2f, y0 = viewH * 0.85f;
            final int n = 12;
            send(t0, t0, MotionEvent.ACTION_DOWN, x, y0);
            for (int i = 1; i <= n; i++) {
                final int k = i;
                h.postDelayed(new Runnable() { public void run() {
                    if (done) return;
                    send(t0, android.os.SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, x, y0 - d * k / (float) n);
                }}, 16L * i);
            }
            h.postDelayed(new Runnable() { public void run() {
                if (done) return;
                send(t0, android.os.SystemClock.uptimeMillis(), MotionEvent.ACTION_MOVE, x, y0 - d);
            }}, 16L * n + 120);
            h.postDelayed(new Runnable() { public void run() {
                if (done) return;
                send(t0, android.os.SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y0 - d);
                then.run();
            }}, 16L * n + 200);
        }

        void send(long down, long t, int action, float x, float y) {
            try {
                MotionEvent e = MotionEvent.obtain(down, t, action, x, y, 0);
                e.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);
                wv.dispatchTouchEvent(e);
                e.recycle();
            } catch (Throwable ex) {}
        }

        // Décalage réel entre deux écrans : rangée y du nouvel écran = rangée y + s de l'ancien.
        // Comparaison en niveaux de gris sur 48 colonnes, tolérante aux petites différences d'affichage.
        static final int COLS = 48;
        int[] sig(Bitmap b) {
            int w = b.getWidth(), hh = b.getHeight();
            int[] px = new int[w], r = new int[hh * COLS];
            for (int y = 0; y < hh; y++) {
                b.getPixels(px, 0, w, 0, y, w, 1);
                for (int c = 0; c < COLS; c++) {
                    int p = px[(int) ((c + 0.5f) * w / COLS)];
                    r[y * COLS + c] = (((p >> 16) & 255) * 3 + ((p >> 8) & 255) * 6 + (p & 255)) / 10;
                }
            }
            return r;
        }

        // Bandeaux fixes (menu collant en haut, barre en bas) : lignes identiques entre deux écrans, exclues de la comparaison et du collage.
        int fixTop, fixBot;
        boolean sameRow(int[] a, int[] b, int y) { long t = 0; int o = y * COLS; for (int c = 0; c < COLS; c++) t += Math.abs(a[o + c] - b[o + c]); return t < COLS * 2; }

        float diff(int[] a, int[] b, int hh, boolean[] vb, int s) {
            long sum = 0; int n = 0;
            for (int y = fixTop; y + s < hh - fixBot; y += 3) {
                if (!vb[y]) continue;
                int ob = y * COLS, oa = (y + s) * COLS;
                for (int c = 0; c < COLS; c++) sum += Math.abs(b[ob + c] - a[oa + c]);
                n += COLS;
            }
            return n < COLS * 15 ? 1e9f : sum / (float) n;
        }

        int shift(Bitmap a, Bitmap b, int expected) {
            int hh = a.getHeight();
            int[] sa = sig(a), sb = sig(b);
            boolean[] vb = new boolean[hh];
            for (int y = 0; y < hh; y++) { int o = y * COLS, mn = 255, mx = 0; for (int c = 0; c < COLS; c++) { mn = Math.min(mn, sb[o + c]); mx = Math.max(mx, sb[o + c]); } vb[y] = mx - mn > 12; }
            fixTop = 0; fixBot = 0;
            if (diff(sa, sb, hh, vb, 0) < 0.8f) return 0; // écran identique : la page ne bouge plus
            while (fixTop < hh / 3 && sameRow(sa, sb, fixTop)) fixTop++;
            while (fixBot < hh / 4 && sameRow(sa, sb, hh - 1 - fixBot)) fixBot++;
            if (fixTop < 3) fixTop = 0;
            if (fixBot < 3) fixBot = 0;
            int best = -1; float bd = 1e9f;
            for (int s = 1; s < hh - fixTop - fixBot - 60; s += 2) { float d = diff(sa, sb, hh, vb, s) + 1.5f * Math.abs(s - expected) / (float) expected; if (d < bd) { bd = d; best = s; } }
            if (best > 1) for (int s = best - 1; s <= best + 1; s += 2) { float d = diff(sa, sb, hh, vb, s) + 1.5f * Math.abs(s - expected) / (float) expected; if (d < bd) { bd = d; best = s; } }
            int slop = android.view.ViewConfiguration.get(act).getScaledTouchSlop();
            if (best < 0 || bd > 19.5f) return Math.max(1, expected - slop); // comparaison impossible : on se fie au glissement
            return best;
        }

        // Fin de l'article visible à l'écran : l'image est coupée juste après, la copie s'arrête.
        boolean endCut;
        void checkEnd(final Runnable cont) {
            wv.evaluateJavascript("(function(){try{return window.__kend?window.__kend():-1}catch(e){return -1}})()", new ValueCallback<String>() {
                public void onReceiveValue(String v) {
                    if (done || finishing) return;
                    float b = -1;
                    try { b = Float.parseFloat(v.replace("\"", "")); } catch (Throwable e) {}
                    if (b > 0) {
                        int px = Math.round(b * wv.getScale()) + dp(24);
                        if (px <= viewH - fixBot) {
                            int end = outY - (viewH - fixBot) + px;
                            if (end > viewH / 2 && end < outY) outY = end;
                            endCut = true;
                            manualFinish();
                            return;
                        }
                    }
                    cont.run();
                }
            });
        }

        void manualFinish() {
            if (done || finishing) return;
            finishing = true;
            try {
                int hh = Math.max(1, Math.min(outY, outH));
                Bitmap cut = Bitmap.createBitmap(out, 0, 0, viewW, hh);
                if (cut != out) { out.recycle(); out = cut; }
            } catch (Throwable e) {}
            strips = Math.max(strips, 1); blanks = 0;
            mode = "copie de l'écran pendant le défilement, " + (steps + 1) + " écrans" + (endCut ? ", jusqu'à la fin de l'article" : "");
            save();
        }

        boolean uniform(Bitmap b, int y0, int y1) {
            int w = b.getWidth(); y1 = Math.min(y1, b.getHeight());
            if (y1 - y0 < 24) return false;
            int ref = b.getPixel(w / 2, y0);
            for (int i = 1; i < 16; i++) for (int j = 0; j < 40; j++)
                if (b.getPixel(w * i / 16, y0 + (y1 - y0 - 1) * j / 39) != ref) return false;
            return true;
        }

        @SuppressWarnings("deprecation")
        void beginStrips() {
            if (done) return;
            try {
                viewW = wv.getWidth(); viewH = wv.getHeight();
                float scale = wv.getScale(); // pixels écran par px CSS
                int contentCss = Math.min(wv.getContentHeight(), maxCssHeight);
                outH = Math.max(viewH, Math.round(contentCss * scale));
                // ARGB_8888 : l'encodage JPEG d'une image RGB_565 sortait noir sur certains téléphones.
                out = Bitmap.createBitmap(viewW, outH, Bitmap.Config.ARGB_8888);
                outCanvas = new Canvas(out);
                outCanvas.drawColor(Color.WHITE);
                strip = Bitmap.createBitmap(viewW, viewH, Bitmap.Config.ARGB_8888);
                label.setText("Capture de la page en cours… (défilement)");
            } catch (Throwable e) { fail("mémoire insuffisante"); return; }
            wv.scrollTo(0, 0);
            step(0, true);
        }

        // Capture par défilement : chaque écran est copié et collé à sa place dans l'image de la page entière.
        void step(final int y, final boolean first) {
            if (done) return;
            wv.scrollTo(0, y);
            h.postDelayed(new Runnable() { public void run() {
                if (done) return;
                final int sy = wv.getScrollY();
                grab(new Runnable() { public void run() {
                    if (done) return;
                    if (first) {
                        // Après le premier écran, les bandeaux fixes (menus, barres collantes) sont masqués pour ne pas se répéter.
                        wv.evaluateJavascript("(function(){var a=document.querySelectorAll('body *');for(var i=0;i<a.length;i++){"
                            + "var p=getComputedStyle(a[i]).position;if(p==='fixed'||p==='sticky')a[i].style.setProperty('visibility','hidden','important');}})()", null);
                    }
                    int next = sy + viewH - dp(2);
                    if (sy + viewH >= outH || next <= sy) save(); else step(next, false);
                }}, sy);
            }}, first ? 700 : 450);
        }

        int blanks, strips;

        // Copie d'un écran : dessin du WebView (rendu logiciel), sinon copie des pixels affichés (Android 8+).
        void grab(final Runnable then, final int sy) {
            strips++;
            blankStrip = false;
            // Copie manuelle : d'abord les pixels réellement affichés (ce que voit l'utilisateur), sinon le dessin du WebView.
            if ((!manual || Build.VERSION.SDK_INT < 26) && drawStrip() && !uniform(strip)) { paste(sy); then.run(); return; }
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    int[] loc = new int[2];
                    wv.getLocationInWindow(loc);
                    Rect r = new Rect(loc[0], loc[1], loc[0] + viewW, loc[1] + viewH);
                    Class<?> pc = Class.forName("android.view.PixelCopy");
                    Class<?> lis = Class.forName("android.view.PixelCopy$OnPixelCopyFinishedListener");
                    Object l = Proxy.newProxyInstance(lis.getClassLoader(), new Class<?>[] { lis }, new InvocationHandler() {
                        public Object invoke(Object o, Method m, Object[] a) {
                            if (!"onPixelCopyFinished".equals(m.getName())) return null;
                            int res = (Integer) a[0];
                            if (res == 0 && !uniform(strip)) paste(sy);
                            else { blanks++; blankStrip = !(drawStrip() && !uniform(strip)); paste(sy); }
                            then.run();
                            return null;
                        }
                    });
                    Method req = pc.getMethod("request", Window.class, Rect.class, Bitmap.class, lis, Handler.class);
                    req.invoke(null, act.getWindow(), r, strip, l, h);
                    return;
                } catch (Throwable e) { /* repli ci-dessous */ }
            }
            blanks++;
            blankStrip = !(drawStrip() && !uniform(strip));
            paste(sy);
            then.run();
        }

        // Dessine la partie affichée du WebView dans « strip », comme Android le fait à l'écran.
        boolean drawStrip() {
            try {
                strip.eraseColor(Color.WHITE);
                Canvas c = new Canvas(strip);
                c.translate(-wv.getScrollX(), -wv.getScrollY());
                wv.draw(c);
                return true;
            } catch (Throwable e) { return false; }
        }

        boolean blankStrip;
        void paste(int sy) { if (sy >= 0) outCanvas.drawBitmap(strip, 0, sy, null); }

        // Image d'une seule couleur = rien n'a été dessiné.
        boolean uniform(Bitmap b) {
            int w = b.getWidth(), hh = b.getHeight(), ref = b.getPixel(w / 2, hh / 2);
            for (int i = 1; i < 12; i++) for (int j = 1; j < 24; j++)
                if (b.getPixel(w * i / 12, hh * j / 24) != ref) return false;
            return true;
        }

        void save() {
            if (done) return;
            if (blanks >= strips || uniform(out)) { fail("le téléphone n'a pas pu copier la page (image vide, " + blanks + "/" + strips + ")"); return; }
            try {
                long ts = System.currentTimeMillis();
                File f = new File(act.getCacheDir(), "kiosque-shot-" + ts + ".jpg");
                FileOutputStream os = new FileOutputStream(f);
                out.compress(Bitmap.CompressFormat.JPEG, 95, os);
                os.close();
                // Vérification : le JPEG relu doit contenir la page ; sinon on enregistre en PNG.
                android.graphics.BitmapFactory.Options o8 = new android.graphics.BitmapFactory.Options();
                o8.inSampleSize = 4;
                Bitmap back = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath(), o8);
                if (back == null || uniform(back)) {
                    f.delete();
                    f = new File(act.getCacheDir(), "kiosque-shot-" + ts + ".png");
                    os = new FileOutputStream(f);
                    out.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.close();
                }
                if (back != null) back.recycle();
                int w = out.getWidth(), hh = out.getHeight();
                // Anciennes captures supprimées.
                File[] old = act.getCacheDir().listFiles();
                if (old != null) for (File o : old)
                    if (o.getName().startsWith("kiosque-shot-") && !o.equals(f) && System.currentTimeMillis() - o.lastModified() > 3600000) o.delete();
                JSObject r = new JSObject();
                r.put("path", f.getAbsolutePath());
                r.put("width", w);
                r.put("height", hh);
                r.put("blanks", blanks);
                r.put("strips", strips);
                r.put("mode", mode);
                finish();
                call.resolve(r);
            } catch (Throwable e) { fail(e.toString()); }
        }

        void fail(String why) {
            if (done) return;
            finish();
            call.reject(why);
        }

        void finish() {
            done = true;
            h.removeCallbacksAndMessages(null);
            try { if (prevOrient != -99) act.setRequestedOrientation(prevOrient); } catch (Throwable e) {}
            try { root.removeView(overlay); wv.stopLoading(); wv.destroy(); } catch (Throwable e) {}
            try { if (out != null) out.recycle(); if (strip != null) strip.recycle(); } catch (Throwable e) {}
        }
    }
}
