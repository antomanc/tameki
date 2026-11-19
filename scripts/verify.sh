#!/usr/bin/env bash
# The one command that runs every check. CI runs exactly this; run it before calling work done.
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/check-conventions.py
./gradlew test lintDebug assembleDebug assembleRelease --stacktrace
