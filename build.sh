#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
case "${1-}" in
    ''|--test) ;;
    *) echo "Usage: ./build.sh [--test]" >&2; exit 2 ;;
esac

mkdir -p build
rm -rf build/classes build/test-classes
mkdir -p build/classes
find src -type f -name '*.java' | LC_ALL=C sort > build/sources.txt
flatlaf=lib/flatlaf-3.6.2.jar
expected=32c79dee6d51341a3f8603598415841b4461fe535de2c34bcc8ee40e91e72267
if command -v sha256sum >/dev/null 2>&1; then
    actual=$(sha256sum "$flatlaf" | cut -d ' ' -f 1)
else
    actual=$(shasum -a 256 "$flatlaf" | cut -d ' ' -f 1)
fi
[ "$actual" = "$expected" ] || { echo "FlatLaf checksum mismatch" >&2; exit 1; }
javac -Xlint:all --release 21 -encoding UTF-8 -cp "$flatlaf" -d build/classes @build/sources.txt
mkdir -p build/classes/assets
cp assets/chess-swing.png build/classes/assets/
(cd build/classes && jar --extract --file "../../$flatlaf")
rm -f build/classes/META-INF/MANIFEST.MF build/classes/module-info.class
printf 'Implementation-Version: %s\nMulti-Release: true\n' "$(cat VERSION)" > build/manifest.mf
jar --create --date=2026-11-24T00:00:00Z --file build/chess-swing.jar --manifest build/manifest.mf \
    --main-class oopchesscode.game.Game -C build/classes .
if [ "${1-}" = --test ]; then
    mkdir -p build/test-classes
    find tests -type f -name '*.java' | LC_ALL=C sort > build/test-sources.txt
    javac -Xlint:all --release 21 -encoding UTF-8 -cp build/classes -d build/test-classes @build/test-sources.txt
    java -cp build/classes:build/test-classes oopchesscode.game.ChessMatchTest
    java -cp build/classes:build/test-classes oopchesscode.game.PgnTest
    java -cp build/classes:build/test-classes oopchesscode.game.PortfolioTest
    java -cp build/classes:build/test-classes oopchesscode.game.LearningTest
fi
