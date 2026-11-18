#!/usr/bin/env bash
# Boots the ameki_fold AVD headless, installs the debug build and opens it, ready for agent-device.
#   scripts/emulator.sh          boot + install + launch
#   scripts/emulator.sh stop     shut the emulator down (it takes ~4 GB of RAM)
set -euo pipefail
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$SDK/platform-tools:$SDK/emulator:$PATH"
cd "$(dirname "$0")/.."

if [[ "${1:-}" == stop ]]; then adb emu kill; exit; fi

if ! adb get-state >/dev/null 2>&1; then
  nohup emulator -avd ameki_fold -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot-save \
    > "${TMPDIR:-/tmp}/ameki-emulator.log" 2>&1 &
  adb wait-for-device
fi
until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; do sleep 5; done

./gradlew -q :app:assembleDebug
./gradlew -q --stop   # free the daemon's RAM before the emulator settles
adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb shell am start -n dev.antomanc.ameki.debug/dev.antomanc.ameki.MainActivity >/dev/null
echo "Ready: agent-device ... --platform android --serial $(adb get-serialno)"
