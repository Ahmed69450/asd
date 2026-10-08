# Offline Hybrid Voice Assistant for BYD DiLink Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-grade, 100% native Kotlin offline hybrid voice assistant for BYD DiLink car head units (Android 10+) featuring a continuous Foreground Service with Vosk Wake Word, Engine A fast ONNX semantic hardware router ($\ge 0.70$), Engine B llama.cpp Tiny LLM fallback (2-3 CPU threads), MCP client layer (Web Search & Accessibility screen awareness), dynamic GPS/time context, Room DB memory RAG, and voice-first Piper TTS formatting.

**Architecture:** Clean Architecture + MVVM with reactive Kotlin Coroutines/Flow. Audio pipeline switches between low-power Vosk keyword spotting and full ASR. Dual-Engine brain dispatches vehicle hardware intents instantly via ONNX embeddings (<20ms) or falls back to local GGUF LLM with dynamic context injection and MCP tools. Voice-first TTS formatter sanitizes output before Piper ONNX speech synthesis with audio ducking.

**Tech Stack:** Kotlin 1.9+, Android SDK 34 (minSdk 24), AndroidX Jetpack, Coroutines & Flow, Room Database, Jetpack DataStore, Microsoft ONNX Runtime Android, Vosk Android ASR, OkHttp 4 (JSON-RPC & SSE), Google Play Services Location, Piper TTS / Android TextToSpeech.

**Spec:** `docs/superpowers/specs/2026-10-08-offline-hybrid-car-voice-assistant-design.md`

## Global Constraints

- **Language & Runtime:** 100% Native Kotlin (Zero Chaquopy/Python runtime overhead).
- **Target OS:** Android 10+ (API level 29+, minSdk 24, targetSdk 34) optimized for BYD DiLink car head unit SoC.
- **CPU Thread Constraint:** LLM inference (Engine B) must be strictly clamped to 2-3 CPU threads to prevent car SoC thermal throttling.
- **Engine A Threshold:** Hardware command execution requires Cosine Similarity $\ge 0.70$. Anything $< 0.70$ routes to Engine B.
- **Voice-First Output:** All spoken text must be strictly stripped of markdown, emojis, HTML tags, URLs, and have math/special symbols converted to spoken Arabic words.
- **Audio Lifecycle:** Continuous Foreground Service with `foregroundServiceType="microphone"` and `AudioManager` audio ducking during speech/listening.
- **Extensibility:** MCP client layer must adhere strictly to JSON-RPC 2.0 tool specification for local and remote tool calling.

## Review Focus

1. **Extreme Silence / Cab Noise:** Audio capture must not lock up or trigger infinite recognition loops when engine or road noise is present; Vosk VAD must reset cleanly.
2. **Missing / Unselected GGUF Model:** When no `.gguf` file is selected in Settings, Engine B must return a clear, localized Arabic warning without crashing.
3. **No GPS / Permission Denied:** If location permission is missing or GPS is disabled in the car, `LocationTimeProvider` must gracefully fallback to system date/time without null pointer exceptions.
4. **Invalid Markdown / Malformed LLM Output:** `VoiceTtsFormatter` must handle broken markdown tokens, unclosed brackets, and mixed scripts without regex infinite loops.
5. **Simulated Hardware Fallback:** If BYD system broadcast intents are rejected or run in an emulator, `CarControlManager` must safely report execution status without crashing.

---

### Task 1: Android Project Scaffolding & Gradle Build Setup

**Files:**
- Create: `build.gradle` (root)
- Create: `settings.gradle`
- Create: `gradle.properties`
- Create: `app/build.gradle`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/layout/activity_settings.xml`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`

**Interfaces:**
- Produces: Base Android application configuration with all libraries (Room, DataStore, ONNX Runtime, Vosk, OkHttp, Location).

- [ ] **Step 1: Write root build.gradle, settings.gradle, and gradle.properties**

```groovy
// settings.gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
rootProject.name = "OfflineCarVoiceAssistant"
include ':app'
```

```groovy
// build.gradle (root)
plugins {
    id 'com.android.application' version '8.2.2' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.22' apply false
    id 'org.jetbrains.kotlin.kapt' version '1.9.22' apply false
}
```

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
```

- [ ] **Step 2: Write app/build.gradle with pure native dependencies**

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.kapt'
}

android {
    namespace 'com.ovos.arabicassistant'
    compileSdk 34

    defaultConfig {
        applicationId "com.ovos.arabicassistant"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        
        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        viewBinding true
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // Room Database
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'

    // Jetpack DataStore
    implementation 'androidx.datastore:datastore-preferences:1.0.0'

    // ONNX Runtime Android (Engine A)
    implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.17.0'

    // Vosk Offline ASR
    implementation 'com.alphacephei:vosk-android:0.3.47'

    // OkHttp & SSE for MCP
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:okhttp-sse:4.12.0'

    // Google Play Services Location
    implementation 'com.google.android.gms:play-services-location:21.0.1'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
    testImplementation 'androidx.room:room-testing:2.6.1'
    testImplementation 'org.json:json:20231013'
}
```

- [ ] **Step 3: Write AndroidManifest.xml and Resources**

Register permissions:
`RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`.
Declare `MainActivity`, `SettingsActivity`, `VoiceAssistantForegroundService`, and `CarAccessibilityService`.

- [ ] **Step 4: Verify structure via git status**

Run: `git status`
Expected: shows uncommitted scaffolded files.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle build.gradle gradle.properties app/
git commit -m "chore: scaffold native Kotlin Android project for car voice assistant"
```

---

### Task 2: Domain Layer Entities, Repository Interfaces & Use Cases

**Files:**
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/model/VoiceCommand.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/model/RouteResult.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/model/CarAction.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/model/MemoryFact.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/model/McpToolCall.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/repository/MemoryRepository.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/repository/CarControlRepository.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/repository/SettingsRepository.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/repository/McpRepository.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/usecase/RouteSemanticIntentUseCase.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/usecase/ExecuteCarActionUseCase.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/usecase/RetrieveMemoryFactsUseCase.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/usecase/ProcessVoiceInputUseCase.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/domain/ProcessVoiceInputUseCaseTest.kt`

**Interfaces:**
- Produces:
  - `ProcessVoiceInputUseCase(router, carRepo, memoryRepo, mcpRepo, llmEngine)`
  - `RouteSemanticIntentUseCase(onnxRouter)`
  - `ExecuteCarActionUseCase(carRepo)`

- [ ] **Step 1: Write the failing unit test for ProcessVoiceInputUseCase**

```kotlin
// ProcessVoiceInputUseCaseTest.kt
package com.ovos.arabicassistant.domain

import com.ovos.arabicassistant.domain.model.*
import com.ovos.arabicassistant.domain.usecase.ProcessVoiceInputUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessVoiceInputUseCaseTest {
    @Test
    fun `when confidence is 70 percent or higher routes to car hardware directly`() = runBlocking {
        // verify router directs to car hardware execution
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.domain.ProcessVoiceInputUseCaseTest`
Expected: FAIL (classes not yet created).

- [ ] **Step 3: Implement domain models, repository interfaces, and use cases**

Define `VoiceCommand(rawText: String, timestamp: Long)`.
Define `RouteResult(intent: String, confidence: Float, isHardware: Boolean, parameters: Map<String, String>)`.
Define `CarAction(type: CarActionType, value: String)`.
Implement `ProcessVoiceInputUseCase` routing logic:
- Check semantic match from Engine A.
- If `confidence >= 0.70f && isHardware`, execute car action via `CarControlRepository` and return spoken confirmation.
- If `confidence < 0.70f`, fetch relevant memory facts from `MemoryRepository`, query MCP tools if needed, and invoke Engine B (LLM).

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.domain.ProcessVoiceInputUseCaseTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ovos/arabicassistant/domain/ app/src/test/java/com/ovos/arabicassistant/domain/
git commit -m "feat(domain): add clean architecture domain models, repositories and use cases"
```

---

### Task 3: Voice-First TTS Formatter & Audio Focus Management

**Files:**
- Create: `app/src/main/java/com/ovos/arabicassistant/voice/VoiceTtsFormatter.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/voice/AudioFocusManager.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/voice/PiperTtsManager.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/voice/VoiceTtsFormatterTest.kt`

**Interfaces:**
- Produces:
  - `VoiceTtsFormatter.sanitize(rawText: String): String`
  - `AudioFocusManager.requestDucking(): Boolean`
  - `AudioFocusManager.abandonFocus()`
  - `PiperTtsManager.speak(text: String, onComplete: () -> Unit)`

- [ ] **Step 1: Write the failing test for VoiceTtsFormatter**

```kotlin
// VoiceTtsFormatterTest.kt
package com.ovos.arabicassistant.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTtsFormatterTest {
    @Test
    fun `strips markdown asterisks hashes and code blocks`() {
        val input = "### درجة الحرارة **25°C** ومستوى الشحن `80%`!"
        val expected = "درجة الحرارة 25 درجة مئوية ومستوى الشحن 80 بالمئة!"
        assertEquals(expected, VoiceTtsFormatter.sanitize(input))
    }

    @Test
    fun `strips all emojis and replaces newlines with single space`() {
        val input = "مرحباً يا سائق 👋\nالطقس مشمس اليوم ☀️\nأتمنى لك رحلة آمنة 🚗"
        val expected = "مرحباً يا سائق الطقس مشمس اليوم أتمنى لك رحلة آمنة"
        assertEquals(expected, VoiceTtsFormatter.sanitize(input))
    }

    @Test
    fun `replaces math and special symbols with arabic words`() {
        val input = "السرعة +5 كم/س & الضغط -2"
        val expected = "السرعة زائد 5 كم/س و الضغط ناقص 2"
        assertEquals(expected, VoiceTtsFormatter.sanitize(input))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.voice.VoiceTtsFormatterTest`
Expected: FAIL.

- [ ] **Step 3: Implement VoiceTtsFormatter, AudioFocusManager, and PiperTtsManager**

Write `VoiceTtsFormatter` with regex patterns for:
- Markdown stripping: `[*_~`#>\[\]\(\)]`, `(?m)^#{1,6}\s*`
- Emoji ranges: `[\x{1F600}-\x{1F64F}\x{1F300}-\x{1F5FF}\x{1F680}-\x{1F6FF}\x{2600}-\x{26FF}\x{2700}-\x{27BF}]`
- Symbol translation: `%` -> `" بالمئة "`, `°C` -> `" درجة مئوية "`, `+` -> `" زائد "`, `-` -> `" ناقص "`, `&` -> `" و "`
- Newline normalization: `[\r\n\t]+` -> `" "`
- Consecutive space collapse: `\s+` -> `" "`
Implement `AudioFocusManager` using `AudioManager.requestAudioFocus` with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
Implement `PiperTtsManager` wrapping system Android `TextToSpeech` and `.onnx` Piper models with `length_scale = 1.15f`.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.voice.VoiceTtsFormatterTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ovos/arabicassistant/voice/ app/src/test/java/com/ovos/arabicassistant/voice/
git commit -m "feat(voice): add VoiceTtsFormatter, AudioFocusManager and PiperTtsManager"
```

---

### Task 4: Data Layer - Room Database & Jetpack DataStore

**Files:**
- Create: `app/src/main/java/com/ovos/arabicassistant/data/local/db/MemoryEntity.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/local/db/MemoryDao.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/local/db/AppDatabase.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/local/repository/MemoryRepositoryImpl.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/local/preferences/SettingsDataStore.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/local/repository/SettingsRepositoryImpl.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/data/local/MemoryDaoTest.kt`

**Interfaces:**
- Produces:
  - `MemoryDao.searchFactsByKeywords(query: String): List<MemoryEntity>`
  - `MemoryRepository.getRelevantFacts(query: String): List<MemoryFact>`
  - `SettingsDataStore.modelPathFlow: Flow<String?>`
  - `SettingsDataStore.cpuThreadsFlow: Flow<Int>` (default 2, clamp 2..3)
  - `SettingsDataStore.speechRateFlow: Flow<Float>`

- [ ] **Step 1: Write the failing test for MemoryDao keyword search**

```kotlin
// MemoryDaoTest.kt
package com.ovos.arabicassistant.data.local

import androidx.room.Room
import com.ovos.arabicassistant.data.local.db.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MemoryDaoTest {
    // Room in-memory testing for storing and querying facts by keywords
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.local.MemoryDaoTest`
Expected: FAIL.

- [ ] **Step 3: Implement Room Entity, DAO, AppDatabase, and DataStore preferences**

Define `MemoryEntity(@PrimaryKey autoGenerate, category, keywords, factText, timestamp)`.
Define `MemoryDao` with `@Query("SELECT * FROM memory_facts WHERE keywords LIKE '%' || :keyword || '%' OR fact_text LIKE '%' || :keyword || '%' LIMIT :limit")`.
Create `SettingsDataStore` with keys for `MODEL_PATH`, `CPU_THREADS` (clamped to 2..3), and `SPEECH_RATE`.
Implement `MemoryRepositoryImpl` and `SettingsRepositoryImpl`.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.local.MemoryDaoTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ovos/arabicassistant/data/local/ app/src/test/java/com/ovos/arabicassistant/data/local/
git commit -m "feat(data): implement Room long-term memory RAG and DataStore preferences"
```

---

### Task 5: Dynamic Context Provider (GPS Location & Time)

**Files:**
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/model/DynamicContext.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/location/LocationTimeProvider.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/domain/usecase/GetDynamicContextUseCase.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/data/location/LocationTimeProviderTest.kt`

**Interfaces:**
- Produces:
  - `LocationTimeProvider.getCurrentLocationAndContext(): DynamicContext`
  - `GetDynamicContextUseCase(locationTimeProvider, memoryRepo): String` (generates formatted prompt prefix)

- [ ] **Step 1: Write the failing test for DynamicContext formatting**

```kotlin
// LocationTimeProviderTest.kt
package com.ovos.arabicassistant.data.location

import com.ovos.arabicassistant.domain.model.DynamicContext
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationTimeProviderTest {
    @Test
    fun `formats system context block with time date and location`() {
        val context = DynamicContext(
            time = "14:30",
            date = "2026-10-08",
            latitude = 24.7136,
            longitude = 46.6753,
            city = "الرياض"
        )
        val formatted = context.toPromptBlock()
        assertTrue(formatted.contains("14:30"))
        assertTrue(formatted.contains("الرياض"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.location.LocationTimeProviderTest`
Expected: FAIL.

- [ ] **Step 3: Implement LocationTimeProvider and GetDynamicContextUseCase**

Implement `LocationTimeProvider` using `FusedLocationProviderClient` with graceful fallback when permissions are not granted or GPS is disabled.
Implement `DynamicContext.toPromptBlock()` producing clean text block:
`[سياق النظام: الوقت الحالي 14:30، التاريخ 2026-10-08، المدينة الرياض]`

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.location.LocationTimeProviderTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ovos/arabicassistant/data/location/ app/src/test/java/com/ovos/arabicassistant/data/location/
git commit -m "feat(location): add LocationTimeProvider and DynamicContext prompt builder"
```

---

### Task 6: Engine A - Fast Semantic Hardware Router (ONNX Runtime 384D)

**Files:**
- Create: `app/src/main/assets/intent_embeddings.json`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/ai/engine_a/AnchorVector.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/ai/engine_a/CosineSimilarity.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/ai/engine_a/OnnxSemanticRouter.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/data/ai/engine_a/OnnxSemanticRouterTest.kt`

**Interfaces:**
- Produces:
  - `OnnxSemanticRouter.route(utterance: String, embedding: FloatArray?): RouteResult`
  - `CosineSimilarity.compute(a: FloatArray, b: FloatArray): Float`
  - Constants: `CONFIDENCE_THRESHOLD = 0.70f`

- [ ] **Step 1: Write failing test for CosineSimilarity and Router threshold**

```kotlin
// OnnxSemanticRouterTest.kt
package com.ovos.arabicassistant.data.ai.engine_a

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnnxSemanticRouterTest {
    @Test
    fun `cosine similarity calculation is exact for identical and orthogonal vectors`() {
        val vec1 = floatArrayOf(1f, 0f, 0f)
        val vec2 = floatArrayOf(1f, 0f, 0f)
        val vec3 = floatArrayOf(0f, 1f, 0f)
        assertEquals(1.0f, CosineSimilarity.compute(vec1, vec2), 0.001f)
        assertEquals(0.0f, CosineSimilarity.compute(vec1, vec3), 0.001f)
    }

    @Test
    fun `routes to hardware when similarity is 70 percent or higher`() {
        // test threshold filtering
    }

    @Test
    fun `falls back to LLM when similarity is below 70 percent`() {
        // test fallback filtering
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.ai.engine_a.OnnxSemanticRouterTest`
Expected: FAIL.

- [ ] **Step 3: Implement CosineSimilarity, anchor embeddings, and OnnxSemanticRouter**

Write `CosineSimilarity.compute(a: FloatArray, b: FloatArray): Float`.
Populate `app/src/main/assets/intent_embeddings.json` with car hardware anchor vectors for:
- `ac_temp_up`, `ac_temp_down`, `ac_fan_speed`
- `window_open`, `window_close`, `sunroof_open`, `sunroof_close`
- `volume_up`, `volume_down`, `volume_mute`, `media_next`, `media_prev`, `media_play_pause`
Implement `OnnxSemanticRouter` with JSON loader and strict `0.70f` threshold logic.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.ai.engine_a.OnnxSemanticRouterTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/assets/intent_embeddings.json app/src/main/java/com/ovos/arabicassistant/data/ai/engine_a/ app/src/test/java/com/ovos/arabicassistant/data/ai/engine_a/
git commit -m "feat(engine-a): implement OnnxSemanticRouter with 384D anchor embeddings and 70% threshold"
```

---

### Task 7: Car Hardware Control Layer (BYD DiLink & System Intents)

**Files:**
- Create: `app/src/main/java/com/ovos/arabicassistant/data/car/BydCarIntentDispatcher.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/car/CarControlManager.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/car/CarControlRepositoryImpl.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/data/car/CarControlManagerTest.kt`

**Interfaces:**
- Produces:
  - `CarControlRepository.executeAction(action: CarAction): Boolean`
  - `BydCarIntentDispatcher.dispatchAcCommand(tempDelta: Int): Boolean`
  - `BydCarIntentDispatcher.dispatchWindowCommand(action: String): Boolean`
  - `BydCarIntentDispatcher.dispatchMediaCommand(keyCode: Int): Boolean`

- [ ] **Step 1: Write the failing test for CarControlManager**

```kotlin
// CarControlManagerTest.kt
package com.ovos.arabicassistant.data.car

import com.ovos.arabicassistant.domain.model.CarAction
import com.ovos.arabicassistant.domain.model.CarActionType
import org.junit.Assert.assertTrue
import org.junit.Test

class CarControlManagerTest {
    @Test
    fun `executes volume and media actions safely`() {
        // verify execution returns true and dispatches correct event
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.car.CarControlManagerTest`
Expected: FAIL.

- [ ] **Step 3: Implement BydCarIntentDispatcher, CarControlManager, and CarControlRepositoryImpl**

Implement broadcast intents for BYD DiLink AC (`byd.intent.action.AC_TEMP`), window control, and `AudioManager` / `KeyEvent` actions for volume and playback.
Include simulated fallback logging when running off-vehicle.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.car.CarControlManagerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ovos/arabicassistant/data/car/ app/src/test/java/com/ovos/arabicassistant/data/car/
git commit -m "feat(car): implement BYD DiLink intent dispatcher and CarControlManager"
```

---

### Task 8: Model Context Protocol (MCP) Client Layer & In-App Tools

**Files:**
- Create: `app/src/main/java/com/ovos/arabicassistant/data/mcp/model/McpModels.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/mcp/McpTool.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/mcp/McpToolRegistry.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/mcp/tools/WebSearchTool.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/mcp/tools/ScreenAwarenessTool.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/mcp/transport/McpTransport.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/mcp/transport/InAppMcpTransport.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/mcp/transport/SseMcpTransport.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/mcp/McpClient.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/mcp/McpRepositoryImpl.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/data/mcp/McpClientTest.kt`

**Interfaces:**
- Produces:
  - `McpClient.listTools(): List<McpToolDefinition>`
  - `McpClient.callTool(name: String, arguments: Map<String, Any>): McpToolResult`
  - `WebSearchTool.execute(query: String): String`
  - `ScreenAwarenessTool.execute(): String`

- [ ] **Step 1: Write failing test for McpClient and In-App Tool Execution**

```kotlin
// McpClientTest.kt
package com.ovos.arabicassistant.data.mcp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class McpClientTest {
    @Test
    fun `lists available tools including web search and screen awareness`() = runBlocking {
        // verify tools/list returns WebSearchTool and ScreenAwarenessTool
    }

    @Test
    fun `dispatches tool call via JSON-RPC protocol`() = runBlocking {
        // verify tools/call invokes registered tool
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.mcp.McpClientTest`
Expected: FAIL.

- [ ] **Step 3: Implement JSON-RPC models, McpToolRegistry, InAppMcpTransport, SseMcpTransport, and McpClient**

Define `McpRequest(jsonrpc = "2.0", id, method, params)` and `McpResponse`.
Implement `WebSearchTool` querying DuckDuckGo Instant Answer API via OkHttp.
Implement `ScreenAwarenessTool` retrieving screen context from `ScreenContextHolder`.
Implement `InAppMcpTransport` and `SseMcpTransport`.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.mcp.McpClientTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ovos/arabicassistant/data/mcp/ app/src/test/java/com/ovos/arabicassistant/data/mcp/
git commit -m "feat(mcp): implement JSON-RPC 2.0 McpClient with WebSearch and ScreenAwareness tools"
```

---

### Task 9: Accessibility Service for Screen Awareness

**Files:**
- Create: `app/src/main/java/com/ovos/arabicassistant/accessibility/ScreenContextHolder.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/accessibility/CarAccessibilityService.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/accessibility/ScreenContextHolderTest.kt`

**Interfaces:**
- Produces:
  - `ScreenContextHolder.update(packageName: String, visibleTexts: List<String>)`
  - `ScreenContextHolder.getCurrentSummary(): String`
  - `CarAccessibilityService: AccessibilityService`

- [ ] **Step 1: Write failing test for ScreenContextHolder**

```kotlin
// ScreenContextHolderTest.kt
package com.ovos.arabicassistant.accessibility

import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenContextHolderTest {
    @Test
    fun `summarizes active screen package and UI text nodes concisely`() {
        ScreenContextHolder.update(
            packageName = "com.spotify.music",
            visibleTexts = listOf("Amr Diab", "Nour El Ain", "Play", "Next")
        )
        val summary = ScreenContextHolder.getCurrentSummary()
        assertTrue(summary.contains("com.spotify.music"))
        assertTrue(summary.contains("Amr Diab"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.accessibility.ScreenContextHolderTest`
Expected: FAIL.

- [ ] **Step 3: Implement ScreenContextHolder and CarAccessibilityService**

Implement `CarAccessibilityService` handling `AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED` and `TYPE_WINDOW_STATE_CHANGED`, extracting text from root `AccessibilityNodeInfo` tree, and pushing to `ScreenContextHolder`.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.accessibility.ScreenContextHolderTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ovos/arabicassistant/accessibility/ app/src/test/java/com/ovos/arabicassistant/accessibility/
git commit -m "feat(accessibility): implement CarAccessibilityService and ScreenContextHolder"
```

---

### Task 10: Engine B - Local Tiny LLM Fallback (llama.cpp JNI Wrapper)

**Files:**
- Create: `app/src/main/java/com/ovos/arabicassistant/data/ai/engine_b/LlamaCppBridge.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/data/ai/engine_b/LlamaCppEngine.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/data/ai/engine_b/LlamaCppEngineTest.kt`

**Interfaces:**
- Produces:
  - `LlamaCppEngine.loadModel(path: String, threads: Int = 2): Boolean`
  - `LlamaCppEngine.generateStream(prompt: String, systemContext: String): Flow<String>`
  - `LlamaCppEngine.isModelLoaded(): Boolean`

- [ ] **Step 1: Write failing test for LlamaCppEngine**

```kotlin
// LlamaCppEngineTest.kt
package com.ovos.arabicassistant.data.ai.engine_b

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlamaCppEngineTest {
    @Test
    fun `clamps cpu threads to maximum of 3 threads`() {
        val engine = LlamaCppEngine(testBridge = true)
        engine.loadModel("/sdcard/model.gguf", requestedThreads = 8)
        assertEquals(3, engine.activeThreads)
    }

    @Test
    fun `formats system prompt with dynamic context and streams tokens`() = runBlocking {
        val engine = LlamaCppEngine(testBridge = true)
        val tokens = engine.generateStream("ما هو الطقس؟", "المدينة: الرياض").toList()
        assertTrue(tokens.isNotEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.ai.engine_b.LlamaCppEngineTest`
Expected: FAIL.

- [ ] **Step 3: Implement LlamaCppBridge and LlamaCppEngine**

Implement `LlamaCppEngine` with strict thread clamping:
`activeThreads = requestedThreads.coerceIn(2, 3)`
Build dynamic prompt structure combining system instructions, location/time, memory facts, and user query.
Provide token streaming via `Flow<String>`.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.data.ai.engine_b.LlamaCppEngineTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ovos/arabicassistant/data/ai/engine_b/ app/src/test/java/com/ovos/arabicassistant/data/ai/engine_b/
git commit -m "feat(engine-b): implement LlamaCppEngine with 2-3 thread clamp and streaming Flow"
```

---

### Task 11: Continuous Foreground Service & Vosk Audio Pipeline

**Files:**
- Create: `app/src/main/java/com/ovos/arabicassistant/voice/VoskSpeechManager.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/service/ServiceNotificationManager.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/service/VoiceAssistantForegroundService.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/voice/VoskSpeechManagerTest.kt`

**Interfaces:**
- Produces:
  - `VoskSpeechManager.startListening(onWakeWordDetected: () -> Unit, onFinalResult: (String) -> Unit)`
  - `VoskSpeechManager.stopListening()`
  - `VoiceAssistantForegroundService: Service` (Foreground Service with microphone type)

- [ ] **Step 1: Write failing test for VoskSpeechManager state transitions**

```kotlin
// VoskSpeechManagerTest.kt
package com.ovos.arabicassistant.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoskSpeechManagerTest {
    @Test
    fun `transitions from wake word state to full dictation upon trigger`() {
        // test state machine logic
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.voice.VoskSpeechManagerTest`
Expected: FAIL.

- [ ] **Step 3: Implement VoskSpeechManager, ServiceNotificationManager, and VoiceAssistantForegroundService**

Write `VoskSpeechManager` supporting:
- Phase 1: Grammar-restricted recognition for `["يا سيارة", "مرحبا سيارة", "مساعد سيارة"]`.
- Phase 2: Full ASR recognition with 700ms silence detection VAD.
Implement `VoiceAssistantForegroundService` with notification channel and microphone type.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.voice.VoskSpeechManagerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ovos/arabicassistant/voice/VoskSpeechManager.kt app/src/main/java/com/ovos/arabicassistant/service/ app/src/test/java/com/ovos/arabicassistant/voice/VoskSpeechManagerTest.kt
git commit -m "feat(service): implement continuous Foreground Service and two-phase Vosk audio pipeline"
```

---

### Task 12: Presentation Layer (MVVM ViewModel, MainActivity & SettingsActivity)

**Files:**
- Create: `app/src/main/java/com/ovos/arabicassistant/presentation/state/AssistantUiState.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/presentation/viewmodel/MainViewModel.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/presentation/ui/MainActivity.kt`
- Create: `app/src/main/java/com/ovos/arabicassistant/presentation/ui/SettingsActivity.kt`
- Test: `app/src/test/java/com/ovos/arabicassistant/presentation/MainViewModelTest.kt`

**Interfaces:**
- Produces:
  - `MainViewModel.uiState: StateFlow<AssistantUiState>`
  - `MainViewModel.onUserUtterance(text: String)`
  - `SettingsActivity`: uses `ActivityResultContracts.GetContent()` for `.gguf` selection.

- [ ] **Step 1: Write failing test for MainViewModel state flow**

```kotlin
// MainViewModelTest.kt
package com.ovos.arabicassistant.presentation

import com.ovos.arabicassistant.presentation.state.AssistantUiState
import com.ovos.arabicassistant.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {
    @Test
    fun `initial state is Idle`() = runBlocking {
        // verify initial UiState
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.presentation.MainViewModelTest`
Expected: FAIL.

- [ ] **Step 3: Implement AssistantUiState, MainViewModel, MainActivity, and SettingsActivity**

Implement `AssistantUiState` with `Idle`, `Listening`, `Processing`, `Speaking`, `Error`.
Implement `MainViewModel` coordinating use cases, TTS, and state transitions.
Build `MainActivity` with high-contrast car UI (microphone button, status pill, conversation bubble).
Build `SettingsActivity` with `ActivityResultContracts.GetContent()` to select `.gguf` file, CPU thread toggle (2 or 3 threads), and speech rate slider.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle testDebugUnitTest --tests com.ovos.arabicassistant.presentation.MainViewModelTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ovos/arabicassistant/presentation/ app/src/test/java/com/ovos/arabicassistant/presentation/
git commit -m "feat(presentation): implement MainViewModel, MainActivity and SettingsActivity with GGUF picker"
```

---

### Task 13: End-to-End Pipeline Verification & Full Build Test

**Files:**
- Create: `app/src/test/java/com/ovos/arabicassistant/FullPipelineIntegrationTest.kt`

**Interfaces:**
- Consumes: All modules from Tasks 1-12.
- Produces: Complete end-to-end integration test validating the entire pipeline.

- [ ] **Step 1: Write FullPipelineIntegrationTest**

```kotlin
// FullPipelineIntegrationTest.kt
package com.ovos.arabicassistant

import com.ovos.arabicassistant.domain.model.*
import com.ovos.arabicassistant.voice.VoiceTtsFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullPipelineIntegrationTest {
    @Test
    fun `hardware command routes to CarAction and formats TTS without LLM`() = runBlocking {
        // verify fast route execution
    }

    @Test
    fun `general query falls back to LLM with dynamic context and sanitized speech`() = runBlocking {
        // verify fallback execution
    }
}
```

- [ ] **Step 2: Run all unit and integration tests**

Run: `gradle testDebugUnitTest`
Expected: ALL PASS.

- [ ] **Step 3: Run full APK compilation test**

Run: `gradle assembleDebug`
Expected: BUILD SUCCESSFUL with zero compile errors.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/ovos/arabicassistant/FullPipelineIntegrationTest.kt
git commit -m "test: add FullPipelineIntegrationTest and verify end-to-end voice assistant build"
```
