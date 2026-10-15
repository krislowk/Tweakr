#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"

JAR="$HOME/android.jar"
KEY="$HOME/Tweakr/tweakr.keystore"
PASS="tweakr123"
OUT="$HOME/storage/shared/Tweakr"

rm -rf build && mkdir -p build/classes build/dex "$OUT"

echo "[1/5] Linking manifest"
aapt2 link -o build/base.apk -I "$JAR" \
  --manifest AndroidManifest.xml \
  --min-sdk-version 24 --target-sdk-version 34 \
  --version-code 1 --version-name 1.0

echo "[2/5] Compiling Java"
javac --release 8 -cp "$JAR" -d build/classes $(find src -name '*.java')

echo "[3/5] Making dex"
d8 --lib "$JAR" --min-api 24 --output build/dex $(find build/classes -name '*.class')

echo "[4/5] Packing"
(cd build/dex && zip -q -j ../base.apk classes.dex)

echo "[5/5] Signing"
if [ ! -f "$KEY" ]; then
  keytool -genkey -keystore "$KEY" -alias tweakr -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass "$PASS" -keypass "$PASS" -dname "CN=Tweakr"
fi
apksigner sign --ks "$KEY" --ks-pass "pass:$PASS" \
  --out "$OUT/Tweakr.apk" build/base.apk

echo "Done: $OUT/Tweakr.apk"
