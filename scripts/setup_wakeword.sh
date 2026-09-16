#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS="$ROOT/app/src/main/assets"
mkdir -p "$ASSETS"

BASE="https://github.com/dscripka/openWakeWord/releases/download/v0.5.1"

download() {
  local url="$1"
  local out="$2"
  if [[ -s "$out" ]]; then
    echo "Already present: $out"
    return
  fi
  echo "Downloading $(basename "$out")"
  curl -fL --retry 4 --retry-delay 2 "$url" -o "$out"
}

download "$BASE/melspectrogram.onnx" "$ASSETS/melspectrogram.onnx"
download "$BASE/embedding_model.onnx" "$ASSETS/embedding_model.onnx"
download "$BASE/hey_jarvis_v0.1.onnx" "$ASSETS/hey_jarvis_v0.1.onnx"

echo "Wake-word assets ready."
