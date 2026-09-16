plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jarvis.ai"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jarvis.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "DEFAULT_GEMINI_MODEL", "\"gemini-3.6-flash\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"${project.findProperty("backendBaseUrl") ?: System.getenv("JARVIS_BACKEND_URL") ?: ""}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*")
        resources.pickFirsts.add("META-INF/versions/9/OSGI-INF/MANIFEST.MF")

        // Sherpa-ONNX 1.10.13 and OpenWakeWord 0.1.5 both use
        // ONNX Runtime 1.18.0. They therefore provide the same native
        // library path. Pick one copy only after aligning both versions.
        jniLibs.pickFirsts.add("**/libonnxruntime.so")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

}

ksp {
    arg("room.generateKotlin", "true")
}

val verifySherpaTts = tasks.register("verifySherpaTts") {
    doLast {
        val aar = project.file("libs/sherpa-onnx-1.10.13.aar")
        if (!aar.isFile || aar.length() < 1_000_000L) {
            error(
                "Required Sherpa-ONNX 1.10.13 AAR is missing or invalid: ${aar.relativeTo(project.projectDir)}. " +
                    "Run scripts/setup_sherpa_tts.sh before building."
            )
        }
    }
}

val verifyVoiceAssets = tasks.register("verifyVoiceAssets") {
    doLast {
        val assetsDir = project.file("src/main/assets")
        val requiredFiles = listOf(
            File(assetsDir, "melspectrogram.onnx"),
            File(assetsDir, "embedding_model.onnx"),
            File(assetsDir, "hey_jarvis_v0.1.onnx"),
            File(assetsDir, "jarvis/jarvis-high.onnx"),
            File(assetsDir, "jarvis/tokens.txt")
        )
        val missingFiles = requiredFiles.filterNot { it.isFile && it.length() > 0L }
        val espeakDir = File(assetsDir, "jarvis/espeak-ng-data")
        if (missingFiles.isNotEmpty() || !espeakDir.isDirectory) {
            val missing = missingFiles.joinToString(separator = "\n") { " - ${it.relativeTo(project.projectDir)}" }
            error(
                "Required offline voice assets are missing.\n" +
                    missing +
                    (if (!espeakDir.isDirectory) "\n - ${espeakDir.relativeTo(project.projectDir)}" else "") +
                    "\nRun scripts/setup_wakeword.sh and scripts/setup_jarvis_voice.sh before building."
            )
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifySherpaTts, verifyVoiceAssets)
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")
    implementation("androidx.core:core-ktx:1.19.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    // Offline Piper/VITS TTS engine. Sherpa-ONNX 1.10.13 uses
    // ONNX Runtime 1.18.0, matching OpenWakeWord 0.1.5.
    // This keeps one compatible ONNX Runtime version across both engines.
    implementation(files("libs/sherpa-onnx-1.10.13.aar"))
    // Real on-device wake-word detection via ONNX Runtime 1.18.0.
    implementation("xyz.rementia:openwakeword:0.1.5")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
