#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS="$ROOT_DIR/app/src/main/assets"
required=(
  "$ASSETS/melspectrogram.onnx"
  "$ASSETS/embedding_model.onnx"
  "$ASSETS/hey_jarvis_v0.1.onnx"
  "$ASSETS/jarvis/jarvis-high.onnx"
  "$ASSETS/jarvis/tokens.txt"
  "$ASSETS/jarvis/model-info.json"
)
for f in "${required[@]}"; do
  [[ -s "$f" ]] || { echo "Missing voice asset: $f" >&2; exit 1; }
done
[[ -d "$ASSETS/jarvis/espeak-ng-data" ]] || { echo "Missing English JARVIS eSpeak runtime data" >&2; exit 1; }
[[ ! -e "$ASSETS/jarvis/ar" && ! -e "$ASSETS/jarvis/en_US" ]] || { echo "Unexpected extra language voice assets found" >&2; exit 1; }
echo "Voice assets verified: wake word + English JARVIS only."
