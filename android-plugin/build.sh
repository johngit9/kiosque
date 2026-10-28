#!/bin/bash
set -e
V=$1; cd /tmp/claude-0/w
python3 -I - "$V" <<'PY'
import json,zipfile,sys
V=sys.argv[1]
pj=json.load(open('/tmp/claude-0/w/x/assets/capacitor.plugins.json'))
pj.append({"pkg":"kiosque-pageshot","classpath":"fr.kiosque.rss.PageShotPlugin"})
pjb=(json.dumps(pj,indent='\t')+'\n').encode()
src=zipfile.ZipFile('/tmp/claude-0/w/k.apk'); out=zipfile.ZipFile('/tmp/claude-0/w/unsigned%s.apk'%V,'w')
repl={'assets/public/index.html':open('/tmp/claude-0/w/x/assets/public/index.html','rb').read(),'assets/capacitor.plugins.json':pjb}
for i in src.infolist():
    if i.filename in ('META-INF/CERT.SF','META-INF/CERT.RSA','META-INF/MANIFEST.MF'): continue
    zi=zipfile.ZipInfo(i.filename,i.date_time); zi.compress_type=i.compress_type; zi.external_attr=i.external_attr
    out.writestr(zi,repl.get(i.filename) or src.read(i))
    if i.filename=='classes4.dex':
        zi=zipfile.ZipInfo('classes5.dex',i.date_time); zi.compress_type=i.compress_type
        out.writestr(zi,open('/tmp/claude-0/n/classes5.dex','rb').read())
import os
for fn in ('readability.js','html2canvas.min.js'):
    zi=zipfile.ZipInfo('assets/public/lib/'+fn,(1981,1,1,1,1,0)); zi.compress_type=zipfile.ZIP_DEFLATED
    out.writestr(zi,open('/tmp/claude-0/w/x/assets/public/lib/'+fn,'rb').read())
out.close()
PY
rm -f za$V.apk final$V.apk
zipalign -f -p 4 unsigned$V.apk za$V.apk
apksigner sign --ks /tmp/claude-0/s/kiosque.p12 --ks-pass pass:android --ks-key-alias androiddebugkey --min-sdk-version 22 --out final$V.apk za$V.apk 2>&1 | grep -v 'JAVA_TOOL\|WARNING' || true
apksigner verify --min-sdk-version 22 final$V.apk 2>&1 | grep -v 'JAVA_TOOL\|WARNING' || true
zipalign -c 4 final$V.apk && echo ALIGNED
unzip -p final$V.apk assets/public/index.html > /tmp/claude-0/s/check.html
node /tmp/claude-0/s/smoke.cjs /tmp/claude-0/s/check.html
