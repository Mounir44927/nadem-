# Jarvis AI — GitHub-ready build

This version keeps the supplied offline JARVIS English Piper/Sherpa-ONNX voice only. The Arabic TTS path is not used for assistant replies.

## Voice
- Offline JARVIS voice: `app/src/main/assets/jarvis/`
- Build script: `scripts/setup_jarvis_voice.sh`
- Wake phrase model: `scripts/setup_wakeword.sh` (`Hey Jarvis`)

## Microphone
The foreground service no longer starts wake-word capture from `onCreate`. It starts only on an explicit resume/default service command. Manual SpeechRecognizer use suspends the wake-word engine through the service itself, then waits briefly before opening the microphone.

## Gemini
The app can accept, replace, and remove a Gemini API key from its Connection screen. The key is stored locally using Android Keystore. The direct Gemini model is `gemini-3.6-flash`. A backend can still be configured through `JARVIS_BACKEND_URL`/`backendBaseUrl` as the safer production path.

## Build
GitHub Actions runs unit tests, lint, then `assembleDebug`, and uploads `jarvis-apk`.
