# JARVIS AI — Jarvis AI 2.0

مساعد صوتي يعمل بواجهة عربية وإدخال عربي، مع إخراج صوتي إنجليزي بصوت JARVIS، مدعومًا بـ Gemini AI وWake Word محلي "Hey JARVIS".

## المزايا المنفذة

- RTL عربي حقيقي وواجهة HUD داكنة.
- **الإدخال:** عربي/نصي وصوتي. **الإخراج الصوتي:** English JARVIS فقط (en_GB).
- إعداد أولي للاسم واللقب مع كلمة إيقاظ مدعومة فعليًا بواسطة النموذج المضمن (Hey Jarvis).
- إدخال نصي يعمل دون الشبكة.
- SpeechRecognizer بجلسة قصيرة، مع إعادة محاولة مؤقتة واحدة وfallback للنص.
- صوت JARVIS محلي إنجليزي (en_GB) عبر sherpa-onnx/Piper، مع إعدادات pitch/rate عند توفرها.
- Wake Word كواجهة مستقلة قابلة لإضافة كاشف محلي حقيقي لاحقًا، دون ادّعاء أنه Voice Match أمني.
- Room/SQLite للرسائل والحقائق وذاكرة البحث مع TTL.
- Gemini gateway مباشر، وواجهة Backend gateway، مع Google Search grounding عند طلب البحث.
- معالجة 401/403/429/timeout/no-network دون حلقات لا نهائية.
- LearningPolicy للحفظ الصريح والحقائق المكتشفة.
- GitHub Actions للاختبارات وLint وبناء APK ورفع artifact.
- `build.sh` لرفع المشروع إلى GitHub وانتظار workflow وتنزيل artifact.

## مفتاح Gemini

يمكن إدخال Gemini API Key من داخل التطبيق من شاشة `Gemini والاتصال`، وتغييره أو حذفه لاحقًا. يُخزَّن المفتاح محليًا مشفّرًا باستخدام Android Keystore.

لا تضع مفتاحًا حقيقيًا داخل Git أو في `gradle.properties`. المفتاح الذي يدخله المستخدم داخل التطبيق ليس سرًا محميًا من الاستخراج على جهازه؛ للاستخدام الإنتاجي واسع النطاق، يُفضَّل استخدام Backend لحماية الأسرار.

النموذج المباشر الافتراضي هو `gemini-3.6-flash`. ويمكن استخدام `JARVIS_BACKEND_URL` أو `-PbackendBaseUrl=...` لمسار Backend الاحتياطي.

## GitHub Actions

يستخدم الـworkflow JDK 17 وAGP 9.4 وGradle 9.6، ويجهّز نماذج الصوت وWake Word قبل الاختبارات وLint وبناء `app-debug.apk`، ثم يرفعه كـartifact.

## البناء

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

وللتسليم عبر Termux:

```bash
bash build.sh
```

## ملاحظة Wake Word

لا تعتمد النسخة الأساسية على `SpeechRecognizer` ككاشف استماع دائم. الوضع الافتراضي هو زر الاستماع، بينما خدمة foreground اختيارية ومشروطة بقيود Android والأذونات. كاشف Wake Word محلي فعلي يحتاج نموذج/مكتبة محددة المصدر والترخيص، لذلك لم يتم إدخال نموذج وهمي أو ادعاء دقة غير مقاسة.


## صوت JARVIS (Piper + ONNX)

تم استبدال مسار TTS ليستخدم `sherpa-onnx` لتشغيل نموذج Piper بصيغة ONNX محليًا على Android.

الموديل المستخدم:
`jgkawell/jarvis` — نسخة `high` الإنجليزية البريطانية.

قبل البناء، شغّل:
```bash
./scripts/setup_jarvis_voice.sh
```

ثم:
```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

`build.sh` وGitHub Actions يشغّلان خطوة تجهيز الصوت تلقائيًا.

مهم: النموذج `en_GB` إنجليزي فقط. إدخال المستخدم يمكن أن يكون عربيًا، لكن طبقة الذكاء الاصطناعي تطلب إخراجًا إنجليزيًا قبل إرساله إلى صوت JARVIS.

## Real local wake word

The app uses the `xyz.rementia:openwakeword` Android library with ONNX Runtime for continuous on-device wake-word detection. The GitHub Actions workflow downloads the official `hey_jarvis_v0.1.onnx`, `melspectrogram.onnx`, and `embedding_model.onnx` assets before building.

The bundled pretrained classifier recognizes **"Hey Jarvis"** only. The onboarding flow validates this phrase so the configured phrase cannot diverge from the actual ONNX model. Detection is suspended while Android `SpeechRecognizer` captures the command, then resumed afterward. Continuous listening is opt-in and is never enabled merely because microphone permission was granted.

Sources:
- Re-MENTIA openWakeWord Android: https://github.com/Re-MENTIA/openwakeword-android-kt
- Official openWakeWord models: https://github.com/dscripka/openWakeWord


## Build prerequisites

GitHub Actions downloads the official Sherpa-ONNX 1.10.13 Android AAR before the Android build. Sherpa-ONNX 1.10.13 uses ONNX Runtime 1.18.0, matching OpenWakeWord 0.1.5; the APK therefore packages one compatible ONNX Runtime version and uses a JNI `pickFirst` only for the duplicate copy of that same version.

For a local build, run:

```bash
./scripts/setup_sherpa_tts.sh
./scripts/setup_jarvis_voice.sh
./scripts/setup_wakeword.sh
./scripts/verify_voice_assets.sh
./gradlew assembleDebug
```

The JARVIS voice remains the primary English offline voice. Android system TTS is used only as a safety fallback if the local neural voice cannot initialize or play.
