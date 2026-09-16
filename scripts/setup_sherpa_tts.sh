#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIBS="$ROOT_DIR/app/libs"
AAR="$LIBS/sherpa-onnx-static-link-onnxruntime-1.13.8.aar"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.10.13/sherpa-onnx-static-link-onnxruntime-1.13.8.aar"

command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
mkdir -p "$LIBS"

if [[ -s "$AAR" ]]; then
  echo "Already present: $AAR"
  exit 0
fi

TMP="$AAR.download"
rm -f "$TMP"
trap 'rm -f "$TMP"' EXIT

curl -fL --retry 5 --retry-delay 2 --connect-timeout 20 "$URL" -o "$TMP"
test -s "$TMP" || { echo "Downloaded Sherpa-ONNX AAR is empty." >&2; exit 1; }

# Basic archive integrity check. An AAR is a ZIP.
python3 - "$TMP" <<'PY'
import sys, zipfile
path = sys.argv[1]
with zipfile.ZipFile(path) as z:
    names = set(z.namelist())
    required = {"classes.jar", "AndroidManifest.xml"}
    missing = required - names
    if missing:
        raise SystemExit(f"Invalid Sherpa-ONNX AAR; missing: {sorted(missing)}")
PY

mv "$TMP" "$AAR"
echo "Sherpa-ONNX 1.10.13 AAR ready: $AAR"
