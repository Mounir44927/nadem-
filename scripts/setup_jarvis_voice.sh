#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS="$ROOT_DIR/app/src/main/assets/jarvis"
MODEL_URL="https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvis/high/jarvis-high.onnx"
CONFIG_URL="https://huggingface.co/jgkawell/jarvis/resolve/main/en/en_GB/jarvis/high/jarvis-high.onnx.json"
ESPEAK_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/espeak-ng-data.tar.bz2"
command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
command -v tar >/dev/null || { echo "tar is required" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 1; }
mkdir -p "$ASSETS"
TMP="$ROOT_DIR/.jarvis-tmp"
rm -rf "$TMP"; mkdir -p "$TMP"
trap 'rm -rf "$TMP"' EXIT
curl -L --fail --retry 3 -o "$TMP/jarvis-high.onnx" "$MODEL_URL"
curl -L --fail --retry 3 -o "$TMP/jarvis-high.onnx.json" "$CONFIG_URL"
python3 -c 'import json,sys; cfg=json.load(open(sys.argv[1],encoding="utf-8")); pairs=[]; [pairs.extend((int(i),s) for i in ids) for s,ids in cfg["phoneme_id_map"].items() if s!="\\n"]; pairs.sort(); open(sys.argv[2],"w",encoding="utf-8").write("".join(f"{s} {i}\\n" for i,s in pairs))' "$TMP/jarvis-high.onnx.json" "$ASSETS/tokens.txt"
curl -L --fail --retry 3 -o "$TMP/espeak-ng-data.tar.bz2" "$ESPEAK_URL"
rm -rf "$TMP/espeak-ng-data"; tar -xf "$TMP/espeak-ng-data.tar.bz2" -C "$TMP"
rm -rf "$ASSETS/espeak-ng-data"; cp -a "$TMP/espeak-ng-data" "$ASSETS/espeak-ng-data"
cp "$TMP/jarvis-high.onnx" "$ASSETS/jarvis-high.onnx"
cp "$TMP/jarvis-high.onnx.json" "$ASSETS/model-info.json"
python3 - "$ASSETS/model-info.json" <<'PY'
import json,sys
cfg=json.load(open(sys.argv[1],encoding='utf-8'))
assert cfg.get('language',{}).get('code','en').lower().startswith('en'), 'JARVIS model is not English'
PY
echo "English JARVIS assets ready."
