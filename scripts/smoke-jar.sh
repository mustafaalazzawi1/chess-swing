#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
output=$(printf 'f2 f3\ne7 e5\ng2 g4\nd8 h4\n' | java -jar build/chess-swing.jar --console)
printf '%s\n' "$output" | grep -Fx 'Black wins by checkmate'
jar --list --file build/chess-swing.jar | grep -Fx 'assets/chess-swing.png'
