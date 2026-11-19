#!/usr/bin/env bash
# Regenerate Play Store listing graphics and the static privacy-policy site.
# Does not run as part of verify.sh — store captures are gated behind this script.
set -euo pipefail
cd "$(dirname "$0")/.."

echo "==> Privacy policy site"
scripts/render-privacy-site.sh

echo "==> Store listing graphics (Roborazzi)"
./gradlew :app:recordStoreAssets --stacktrace
# Drop any leftover capture temps if a previous run was interrupted.
find fastlane/metadata/android -name '_raw_*.png' -delete

echo "==> Normalise featureGraphic (no alpha) and verify dimensions"
uv run --with pillow python3 - <<'PY'
from pathlib import Path
from PIL import Image

root = Path("fastlane/metadata/android")
expected = {
    "icon.png": (512, 512),
    "featureGraphic.png": (1024, 500),
}
shot_size = (1080, 2400)
errors = []

for locale in ("it-IT", "en-US"):
    images = root / locale / "images"
    shots_dir = images / "phoneScreenshots"

    for name, size in expected.items():
        path = images / name
        if not path.is_file():
            errors.append(f"missing {path}")
            continue
        im = Image.open(path)
        if name == "featureGraphic.png":
            # Play rejects transparency on the feature graphic.
            if im.mode != "RGB":
                im = im.convert("RGB")
                im.save(path, format="PNG")
                im = Image.open(path)
            if im.mode != "RGB" or ("A" in im.getbands()):
                errors.append(
                    f"{path}: expected RGB without alpha, got mode={im.mode} bands={im.getbands()}"
                )
        elif name == "icon.png":
            # Play asks for a 32-bit PNG for the high-res icon.
            if im.mode != "RGBA":
                im = im.convert("RGBA")
                im.save(path, format="PNG")
                im = Image.open(path)
            if im.mode != "RGBA":
                errors.append(f"{path}: expected RGBA, got mode={im.mode}")
        if im.size != size:
            errors.append(f"{path}: expected {size}, got {im.size}")

    if not shots_dir.is_dir():
        errors.append(f"missing {shots_dir}")
        continue
    shots = sorted(p for p in shots_dir.glob("*.png") if not p.name.startswith("_"))
    if not (2 <= len(shots) <= 8):
        errors.append(f"{shots_dir}: need 2–8 screenshots, found {len(shots)}")
    for shot in shots:
        im = Image.open(shot)
        if im.size != shot_size:
            errors.append(f"{shot}: expected {shot_size}, got {im.size}")

if errors:
    print("\n".join(errors))
    raise SystemExit(1)

print("All store assets OK:")
for locale in ("it-IT", "en-US"):
    images = root / locale / "images"
    for path in sorted(images.rglob("*.png")):
        if path.name.startswith("_"):
            continue
        im = Image.open(path)
        extra = f" mode={im.mode}" if path.name == "featureGraphic.png" else ""
        print(f"  {path}: {im.size[0]}x{im.size[1]}{extra}")
PY

echo "==> Stopping Gradle daemons"
./gradlew --stop

echo "==> Done. Graphics in fastlane/metadata/android/; site in site/"
