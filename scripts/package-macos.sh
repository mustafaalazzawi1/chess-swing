#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ "$(uname -s)" != Darwin ]]; then
    echo "Run this script on macOS with a JDK 21+ matching the target architecture." >&2
    exit 1
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 21+)
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
for tool in java javac jar jpackage sips iconutil ditto; do
    command -v "$tool" >/dev/null || { echo "Missing tool: $tool" >&2; exit 1; }
done

./build.sh --test
./scripts/smoke-jar.sh
staging=$(mktemp -d "${TMPDIR:-/tmp}/chess-swing.XXXXXX")
trap 'rm -rf "$staging"' EXIT
mkdir -p "$staging/input" "$staging/ChessSwing.iconset" "$staging/output"
cp build/chess-swing.jar "$staging/input/"
for size in 16 32 128 256 512; do
    sips -z "$size" "$size" assets/chess-swing.png --out "$staging/ChessSwing.iconset/icon_${size}x${size}.png" >/dev/null
    double=$((size * 2))
    sips -z "$double" "$double" assets/chess-swing.png --out "$staging/ChessSwing.iconset/icon_${size}x${size}@2x.png" >/dev/null
done
iconutil --convert icns --output "$staging/ChessSwing.icns" "$staging/ChessSwing.iconset"
jpackage --type app-image --dest "$staging/output" --input "$staging/input" \
    --name "Chess Swing" --app-version "$(cat VERSION)" --main-jar chess-swing.jar \
    --main-class oopchesscode.game.Game --icon "$staging/ChessSwing.icns" \
    --mac-package-identifier io.github.mustafaalazzawi1.chessswing \
    --mac-package-name "Chess Swing" --mac-app-category public.app-category.board-games
app="$staging/output/Chess Swing.app"
test -d "$app/Contents/runtime"
test -x "$app/Contents/MacOS/Chess Swing"
mkdir -p build/macos
archive="build/macos/ChessSwing-macos-$(uname -m).zip"
ditto -c -k --sequesterRsrc --keepParent "$app" "$staging/ChessSwing.zip"
mv -f "$staging/ChessSwing.zip" "$archive"
printf 'Built %s. Extract it and open Chess Swing.app from Finder to smoke-test.\n' "$archive"
