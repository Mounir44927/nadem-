# ملاحظات التحقق

تم تدقيق شجرة المشروع والملفات المطلوبة، وفحص صياغة ملفات shell/XML، كما تم تمرير ملفات Kotlin المستقلة عن Android عبر مترجم Kotlin المحلي.

تعذر تنفيذ `./gradlew testDebugUnitTest lintDebug assembleDebug` داخل بيئة الفحص لأن DNS الخارجي إلى `services.gradle.org` غير متاح. هذه بيئة الفحص فقط. على GitHub Actions سيستطيع الـbootstrap launcher تنزيل Gradle 9.6 عند توفر الشبكة.

تم قبل التغليف إصلاح إعدادات `packaging` في `app/build.gradle.kts` ووضع `resources.pickFirsts` و`jniLibs.pickFirsts` داخل كتلة `packaging` كما يتطلب DSL الخاص بـAGP. كما تم تحديث README/GITHUB_READY ليتوافقا مع صوت JARVIS الإنجليزي وإدخال مفتاح Gemini من داخل التطبيق.


## Wake-word update
The always-on voice service now uses openWakeWord on-device. The current bundled pretrained model is "Hey Jarvis"; replace the classifier asset with a separately trained Arabic "JARVIS" model when available.
