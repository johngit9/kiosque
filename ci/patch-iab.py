#!/usr/bin/env python3
"""Ajoute un bloqueur de pubs et de traqueurs au lecteur intégré (cordova-plugin-inappbrowser).

Les requêtes vers les domaines de la liste reçoivent une réponse vide. Lancé par
android.yml après npm install, avant que Capacitor copie le plugin dans le projet Android.
"""
import pathlib, sys

SRC = pathlib.Path("node_modules/cordova-plugin-inappbrowser/src/android/InAppBrowser.java")
DOMAINS = """
doubleclick.net googlesyndication.com googleadservices.com google-analytics.com analytics.google.com
googletagmanager.com googletagservices.com adservice.google.com adservice.google.fr 2mdn.net
amazon-adsystem.com adnxs.com criteo.com criteo.net taboola.com outbrain.com outbrainimg.com
smartadserver.com sascdn.com pubmatic.com rubiconproject.com casalemedia.com openx.net indexww.com
teads.tv teads.com quantserve.com quantcount.com scorecardresearch.com chartbeat.com chartbeat.net
moatads.com adsrvr.org yieldlove.com seedtag.com sddan.com xiti.com ati-host.net at-o.net aticdn.net
hotjar.com hotjar.io mouseflow.com facebook.net ads-twitter.com analytics.twitter.com bat.bing.com
clarity.ms krxd.net bluekai.com demdex.net omtrdc.net everesttech.net adform.net adsafeprotected.com
doubleverify.com mgid.com sharethrough.com triplelift.com 3lift.com lijit.com sovrn.com gumgum.com
spotxchange.com spotx.tv springserve.com freewheel.tv stickyadstv.com weborama.fr weborama.com
mediarithmics.com eulerian.net ligatus.com mookie1.com adotmob.com tradedoubler.com awin1.com
nr-data.net segment.io mixpanel.com amplitude.com cxense.com permutive.com permutive.app
rlcdn.com id5-sync.com lngtd.com ad.gt confiant-integrations.net dotmetrics.net gemius.pl estat.com
analytics.tiktok.com ads.linkedin.com px.ads.linkedin.com snap.licdn.com adsystem.com adition.com
smilewanted.com richaudience.com adyoulike.com onetag-sys.com 33across.com districtm.io e-planning.net
quantcast.com tapad.com crwdcntrl.net exelator.com agkn.com bidswitch.net contextweb.com yieldmo.com
adkernel.com media.net servenobid.com sonobi.com undertone.com zemanta.com powerlinks.com
""".split()

PATCH_OLD = """        public WebResourceResponse shouldInterceptRequest(String url, WebResourceResponse response, String method) {
            return response;"""
PATCH_NEW = """        public WebResourceResponse shouldInterceptRequest(String url, WebResourceResponse response, String method) {
            if (KiosqueBlock.isBlocked(url)) return new WebResourceResponse("text/plain", "utf-8", new java.io.ByteArrayInputStream(new byte[0]));
            return response;"""

BLOCK_CLASS = """

/** Kiosque : pubs et traqueurs bloqués dans le lecteur intégré. */
class KiosqueBlock {
    private static final java.util.HashSet<String> HOSTS = new java.util.HashSet<>(java.util.Arrays.asList(
        %s
    ));
    static boolean isBlocked(String url) {
        try {
            String h = android.net.Uri.parse(url).getHost();
            while (h != null && h.contains(".")) {
                if (HOSTS.contains(h)) return true;
                h = h.substring(h.indexOf('.') + 1);
            }
        } catch (Exception e) { }
        return false;
    }
}
""" % ",\n        ".join('"%s"' % d for d in DOMAINS)

src = SRC.read_text()
if "KiosqueBlock" in src:
    sys.exit(0)
if src.count(PATCH_OLD) != 1:
    sys.exit("patch-iab : point d'insertion introuvable dans InAppBrowser.java")
SRC.write_text(src.replace(PATCH_OLD, PATCH_NEW) + BLOCK_CLASS)
print("Bloqueur ajouté : %d domaines" % len(DOMAINS))
