#!/usr/bin/env bash
# Builds the signed release bundle (.aab) for Google Play.
# Signing secrets live outside the repo in ~/.config/tameki/release.env (see docs/play-console.md).
set -euo pipefail
ENV_FILE="${TAMEKI_RELEASE_ENV:-$HOME/.config/tameki/release.env}"
[[ -f "$ENV_FILE" ]] || { echo "Missing $ENV_FILE" >&2; exit 1; }
# shellcheck disable=SC1090
source "$ENV_FILE"
cd "$(dirname "$0")/.."
./gradlew --no-configuration-cache :app:bundleRelease
./gradlew --stop >/dev/null
echo "Bundle: app/build/outputs/bundle/release/app-release.aab"
