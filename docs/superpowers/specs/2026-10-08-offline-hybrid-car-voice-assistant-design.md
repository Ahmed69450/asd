# وثيقة التصميم المعماري: المساعد الصوتي الهجين لشاشات سيارات BYD DiLink
## (Offline Hybrid Voice Assistant for BYD DiLink Head Unit)

**التاريخ:** 08 أكتوبر 2026  
**الحالة:** معتمد من المستخدم (Approved Design)  
**الهدف:** بناء مساعد صوتي متقدم، هجين، فائق السرعة، ومصمم خصيصاً لشاشات سيارات BYD DiLink (نظام Android 10+) بلغة Kotlin بنسبة 100% وبمعمارية نظيفة (Clean Architecture) وخالية من بيئة بايثون تماماً.

---

## 1. ملخص المعمارية والمتطلبات (Architecture Overview)

يعتمد النظام على **المعمارية المعيارية النظيفة (Clean Architecture & MVVM)** مع فصل كامل للمسؤوليات، والاعتماد على الكوروتينز وتدفقات البيانات التفاعلية (`StateFlow` / `SharedFlow`)، مع إدارة صارمة لموارد الذاكرة والمعالج في بيئة السيارة.

### 1.1 محاور النظام الرئيسية:
1. **الاستماع الصوتي وكلمة التنبيه (Foreground Audio Pipeline):**
   * خدمة أمامية مستمرة (`VoiceAssistantForegroundService`) بنوع `microphone`.
   * محرك Vosk ثنائي المراحل: رصد كلمة التنبيه خفيف الموارد (Grammar/Keyword Spotting) ثم الانتقال إلى التعرف الصوتي الكامل (Full ASR).
   * خفض صوت وسائط وراديو السيارة تلقائياً (`AudioFocusManager` مع Audio Ducking).
2. **العقل الهجين ثنائي المحركات (Dual-Engine Brain):**
   * **المحرك A (الموجه الدلالي السريع - Engine A):** استدلال محلي عبر `onnxruntime-android` بنموذج Sentence Transformer (384D) لتصنيف أوامر السيارة في زمن يقل عن 20 مللي ثانية مع اشتراط نسبة ثقة $\ge 70\%$.
   * **المحرك B (النموذج اللغوي الاحتياطي - Engine B):** غلاف JNI لـ `llama.cpp` لتشغيل نماذج `.gguf` الخفيفة على أنوية المعالج (2 إلى 3 Threads فقط) للأسئلة العامة والمعقدة والدردشة.
3. **طبقة عتاد السيارة (Car Hardware Control Layer):**
   * واجهة تنفيذ أوامر التكييف، النوافذ، ومستوى الصوت والوسائط عبر نوايا أندرويد وبث نوايا BYD DiLink.
4. **عميل بروتوكول سياق النماذج (MCP Client Layer):**
   * معمارية عميل MCP متوافقة مع JSON-RPC 2.0 تدعم الأدوات المدمجة داخل التطبيق (البحث المباشر عبر الويب، والوعي بمحتوى الشاشة عبر `AccessibilityService`) مع إمكانية الربط بخوادم خارجية عبر OkHttp SSE.
5. **السياق اللحظي والذاكرة طويلة المدى (Dynamic Context & RAG Memory):**
   * قراءة الموقع الجغرافي الدقيق GPS عبر `FusedLocationProviderClient` وحقن الوقت الفعلي.
   * ذاكرة طويلة المدى عبر Room Database لاسترجاع الحقائق المفتاحية للجولة الحالية فقط وتوفير نافذة السياق.
6. **مُعقّم النطق الصوتي وواجهة اختيار النماذج (Voice-First TTS & Dynamic Model Selector):**
   * تنقية صارمة للمخرجات من الماركداون والرموز التعبيرية وتحويل الرموز الحسابية لكلمات قبل تمريرها لمحرك Piper TTS المبني بنماذج ONNX الصوتية.
   * واجهة إعدادات لاختيار ملفات `.gguf` مباشرة من ذاكرة الجهاز عبر `ActivityResultContracts.GetContent()`.

---

## 2. هيكل الحزم والملفات (Directory & Package Structure)

```
com.ovos.arabicassistant/
├── presentation/
│   ├── viewmodel/
│   │   └── MainViewModel.kt                   # إدارة تدفق الحالات والمدخلات
│   ├── ui/
│   │   ├── MainActivity.kt                    # شاشة المساعد الرئيسية وعرض الحوار
│   │   └── SettingsActivity.kt                # واجهة اختيار نموذج GGUF والإعدادات
│   └── state/
│       └── AssistantUiState.kt                # تمثيل الحالات: Idle, Listening, Processing, Speaking, Error
├── domain/
│   ├── model/
│   │   ├── VoiceCommand.kt                    # نموذج بيانات الأمر الصوتي
│   │   ├── RouteResult.kt                     # نتيجة التوجيه الدلالي ونسبة الثقة
│   │   ├── CarAction.kt                       # تمثيل أوامر السيارة (تكييف، نوافذ، صوت)
│   │   ├── MemoryFact.kt                      # حقيقة مسترجعة من الذاكرة
│   │   └── McpToolCall.kt                     # تمثيل استدعاء أدوات بروتوكول MCP
│   ├── repository/
│   │   ├── MemoryRepository.kt                # واجهة مستودع الذاكرة
│   │   ├── CarControlRepository.kt            # واجهة التحكم بعتاد السيارة
│   │   └── SettingsRepository.kt              # واجهة إعدادات التطبيق
│   └── usecase/
│       ├── ProcessVoiceInputUseCase.kt        # منسق التدفق الصوتي الرئيسي بين المحركات
│       ├── RouteSemanticIntentUseCase.kt      # تقييم عتبة الـ 70% وتوجيه المحرك A
│       ├── ExecuteCarActionUseCase.kt         # تنفيذ أمر السيارة المادي
│       ├── QueryMcpToolsUseCase.kt            # تنفيذ واستدعاء أدوات MCP
│       └── RetrieveMemoryFactsUseCase.kt      # استرجاع الحقائق المفتاحية للجولة
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt                 # قاعدة بيانات Room
│   │   │   ├── MemoryEntity.kt                # جدول حقائق الذاكرة
│   │   │   └── MemoryDao.kt                   # استعلامات الكلمات المفتاحية
│   │   └── preferences/
│   │       └── SettingsDataStore.kt           # تخزين إعدادات النموذج والأداء
│   ├── ai/
│   │   ├── engine_a/
│   │   │   └── OnnxSemanticRouter.kt          # مشغل نموذج MiniLM 384D وحساب Cosine Similarity
│   │   └── engine_b/
│   │       ├── LlamaCppEngine.kt              # غلاف تشغيل نماذج GGUF
│   │       └── LlamaCppBridge.kt              # واجهة JNI الأصلية لـ llama.cpp
│   ├── car/
│   │   ├── CarControlManager.kt               # إدارة أوامر السيارة وتوزيعها
│   │   └── BydCarIntentDispatcher.kt          # نوايا أندرويد وBYD DiLink للبث المباشر
│   ├── location/
│   │   └── LocationTimeProvider.kt            # جلب إحداثيات GPS والوقت الحالي
│   └── mcp/
│       ├── McpClient.kt                       # منسق رسائل JSON-RPC 2.0
│       ├── McpToolRegistry.kt                 # سجل الأدوات المتاحة
│       ├── tools/
│       │   ├── WebSearchTool.kt               # أداة البحث المباشر في الويب
│       │   └── ScreenAwarenessTool.kt         # أداة قراءة محتوى الشاشة
│       └── transport/
│           ├── InAppMcpTransport.kt           # ناقل استدعاء الأدوات المحلية
│           └── SseMcpTransport.kt             # ناقل OkHttp SSE للخوادم البعيدة
├── service/
│   ├── VoiceAssistantForegroundService.kt     # الخدمة الخلفية الأمامية الدائمة
│   └── ServiceNotificationManager.kt          # إدارة إشعار شريط الحالة
├── accessibility/
│   └── CarAccessibilityService.kt             # استخراج شجرة عناصر واجهة المستخدم الحالية
└── voice/
    ├── VoskSpeechManager.kt                   # إدارة التعرف الصوتي ثنائي المراحل (Wake Word + ASR)
    ├── PiperTtsManager.kt                     # تشغيل نماذج ONNX لنطق الصوت مع التراجع التلقائي
    ├── VoiceTtsFormatter.kt                   # تنقية وتجريد المخرجات من الماركداون والرموز
    └── AudioFocusManager.kt                   # إدارة خفض صوت الوسائط (Audio Ducking)
```

---

## 3. المواصفات البرمجية التفصيلية للمكونات (Detailed Specifications)

### 3.1 معالجة الصوت وكلمة التنبيه (`VoskSpeechManager` & `VoiceAssistantForegroundService`)
* **الخدمة الأمامية:**
  * تشغيل دائم مع استهلاك ضئيل للطاقة والذاكرة، مسجلة في `AndroidManifest.xml` بنوع `foregroundServiceType="microphone"`.
  * استقبال أوامر البث لبدء أو إيقاف الاستماع.
* **إدارة التعرف الصوتي (Two-Phase Vosk Recognition):**
  * **Phase 1: Keyword Detection:**
    * تهيئة `org.vosk.Recognizer` بقاموس نحوي محدود: `["يا سيارة", "مرحبا سيارة", "مساعد سيارة"]`.
    * استهلاك المعالج يقل عن $2\%$ مع تجاهل تام لأحاديث الركاب العادية.
  * **Phase 2: Full Utterance Capture:**
    * بمجرد التقاط كلمة التنبيه، إطلاق إشارة صوتية خفيفة وتفعيل `Recognizer` الكامل دون قيود نحوية.
    * تفعيل VAD مؤقت مع نافذة صمت 700ms لتحديد نهاية الحديث وإرسال النتيجة إلى `ProcessVoiceInputUseCase`.

### 3.2 المحرك A: التوجيه الدلالي الفوري (`OnnxSemanticRouter`)
* **المسار:** `data/ai/engine_a/OnnxSemanticRouter.kt`
* **المعمارية:**
  * تحميل نموذج التضمين `semantic_model.onnx` (All-MiniLM-L6-v2 أو نموذج عربي خفيف 384D) عبر `com.microsoft.onnxruntime.OrtEnvironment`.
  * قراءة متجهات النوايا المسبقة للأجهزة من `intent_embeddings.json`.
  * حساب جيب التمام:
    $$\text{Cosine Similarity} = \frac{\mathbf{u} \cdot \mathbf{v}}{\|\mathbf{u}\|_2 \|\mathbf{v}\|_2}$$
  * **قاعدة التوجيه الصارمة:**
    * إذا كانت النتيجة $\ge 0.70$: إرجاع `RouteResult(isHardwareCommand = true, intent = ..., confidence = ...)`.
    * إذا كانت النتيجة $< 0.70$: إرجاع `RouteResult(isHardwareCommand = false, fallbackToLlm = true)`.

### 3.3 المحرك B: النموذج اللغوي المحلي (`LlamaCppEngine`)
* **المسار:** `data/ai/engine_b/`
* **المواصفات:**
  * يدعم ملفات `.gguf` (4-bit quantized، أحجام 0.5B إلى 1.5B).
  * ضبط خيوط المعالجة `threads = 2` (أو 3 كحد أقصى) قابلة للتعديل عبر `SettingsDataStore`.
  * حصر حجم السياق في 1024 أو 2048 توكن لتوفير الرام لشاشة السيارة.
  * تدفق الرد لحظياً باستخدام Kotlin `Flow<String>` لتقليل زمن الاستجابة الأولية (Time to First Token).

### 3.4 طبقة التحكم بالسيارة (`CarControlManager` & `BydCarIntentDispatcher`)
* **المسار:** `data/car/`
* **النوايا المدعومة:**
  * **التكييف:**
    * رفع/خفض درجة الحرارة (`ACTION_AC_SET_TEMP`).
    * تشغيل/إيقاف التكييف ومستوى المروحة (`ACTION_AC_SET_FAN`).
  * **النوافذ وفتحة السقف:**
    * فتح/إغلاق النوافذ (`ACTION_WINDOW_CONTROL`).
    * فتح/إغلاق فتحة السقف (`ACTION_SUNROOF_CONTROL`).
  * **الوسائط ومستوى الصوت:**
    * التفاعل المباشر مع `AudioManager` لرفع أو خفض أو كتم الصوت.
    * إرسال أحداث `KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_MEDIA_NEXT`, `KEYCODE_MEDIA_PREVIOUS`.

### 3.5 بروتوكول سياق النماذج (`McpClient` Layer)
* **المسار:** `data/mcp/`
* **معمارية الرسائل (JSON-RPC 2.0):**
  * `tools/list`: استعراض الأدوات المسجلة مع مواصفات JSON Schema للمعاملات.
  * `tools/call`: تمرير اسم الأداة والمدخلات وإرجاع نتيجة منسقة.
* **الأدوات المدمجة (In-App Tools):**
  1. `WebSearchTool`:
     * المعامل: `query: String`.
     * آلية العمل: طلب استعلام فوري عبر OkHttp من واجهات بيانات خفيفة (DuckDuckGo Instant Answer / Open-Meteo) لإرجاع ملخص رقمي ونصوص موجزة خالية من إعلانات الويب.
  2. `ScreenAwarenessTool`:
     * المعامل: `target_element: String?`.
     * آلية العمل: قراءة شجرة العناصر الحالية المجمعة في `CarAccessibilityService` واستخراج النصوص الظاهرة على الشاشة (مثل التطبيق المفتوح حالياً، أسماء الأغاني المعروضة، أو الرسائل النصية المقروءة).

### 3.6 خدمة إمكانية الوصول لشاشة السيارة (`CarAccessibilityService`)
* **المسار:** `accessibility/CarAccessibilityService.kt`
* **المواصفات:**
  * تسجيل الخدمة في النظام لمراقبة أحداث `TYPE_WINDOW_STATE_CHANGED` و `TYPE_WINDOW_CONTENT_CHANGED`.
  * تجميع نصوص العقد (`AccessibilityNodeInfo`) وتصفية النصوص الفارغة والمكررة، وتحديث كائن الحالة اللحظي `CurrentScreenContext`.
  * توفير سياق الشاشة فورياً لأداة `ScreenAwarenessTool` دون تأخير أو فحص ثقيل متكرر.

### 3.7 السياق الديناميكي والموقع الجغرافي (`LocationTimeProvider`)
* **المسار:** `data/location/LocationTimeProvider.kt`
* **المواصفات:**
  * استخدام `LocationServices.getFusedLocationProviderClient(context)` مع حماية الإذن `ACCESS_FINE_LOCATION`.
  * جلب آخر موقع معروف أو طلب تحديث موقع لحظي.
  * تكوين كتلة السياق الزمني والمكاني:
    ```
    [System Context]
    Current Time: 14:35
    Current Date: 2026-10-08
    Location Coordinates: 24.7136, 46.6753
    City: الرياض
    ```

### 3.8 الذاكرة طويلة المدى المستندة إلى RAG خفيف (`MemoryDao` & Room DB)
* **المسار:** `data/local/db/`
* **مخطط الجدول (`memory_facts`):**
  * `id: Long` (Primary Key, AutoGenerate)
  * `category: String` (مثل: `user_preference`, `routine`, `car_preset`)
  * `keywords: String` (كلمات مفتاحية مفصولة بفواصل)
  * `fact_text: String` (نص الحقيقة، مثال: "درجة حرارة التكييف المفضلة للسائق هي 22 مئوية")
  * `timestamp: Long`
* **آلية الاسترجاع:**
  * البحث عبر استعلام Room باستخدام الكلمات المفتاحية (`LIKE %keyword%`).
  * جلب أفضل 2-3 نتائج ذات صلة فقط وحقنها في الـ Prompt لجولة الحوار الحالية.

### 3.9 معقم النطق الصوتي (`VoiceTtsFormatter`)
* **المسار:** `voice/VoiceTtsFormatter.kt`
* **قواعد التطهير الإلزامية:**
  1. إزالة جميع وسوم الماركداون: `*`, `**`, `###`, `_`, `[ ]`, `( )`, `` ` ``, `>`.
  2. إزالة كافة الرموز التعبيرية (Emoji Unicode Ranges).
  3. استبدال الرموز الحسابية والخاصة بنصوص عربية صريحة:
     * `%` $\rightarrow$ "بالمئة"
     * `°C` $\rightarrow$ "درجة مئوية"
     * `+` $\rightarrow$ "زائد"
     * `-` $\rightarrow$ "ناقص"
     * `&` $\rightarrow$ "و"
  4. استبدال محارف الأسطر الجديدة `\n` و `\r` و `\t` بمسافات فردية لمنع تقطيع تدفق الصوت في محرك Piper TTS.

---

## 4. تدفق البيانات الشامل (End-to-End Data Flow)

```
[صوت السائق في السيارة]
         │
         ▼
[VoskSpeechManager (Wake Word Mode: "يا سيارة")]
         │ (تم الرصد + إشارة صوتية)
         ▼
[VoskSpeechManager (Full ASR Dictation)]
         │ (جملة السائق النصية)
         ▼
[ProcessVoiceInputUseCase]
         │
         ├───► [OnnxSemanticRouter (Engine A: MiniLM 384D)]
         │           │
         │     ┌─────┴─────────────────────────┐
         │     │ الثقة ≥ 0.70 (أمر عتاد)       │ الثقة < 0.70
         │     ▼                               ▼
         │ [ExecuteCarActionUseCase]   [LlamaCppEngine (Engine B)]
         │     │ (نوايا تكييف/نوافذ/صوت)       │
         │     ▼                               ├─► [LocationTimeProvider] (موقع ووقت)
         │ [BydCarIntentDispatcher]            ├─► [RetrieveMemoryFactsUseCase] (حقائق Room)
         │                                     └─► [QueryMcpToolsUseCase]
         │                                             ├─► WebSearchTool
         │                                             └─► ScreenAwarenessTool (Accessibility)
         │                                                     │
         │                                                     ▼
         │                                             [توليد الرد بالـ LLM]
         ▼                                                     │
[صياغة الرد التأكيدي]                                         │
         │                                                     │
         └──────────────────────┬──────────────────────────────┘
                                │
                                ▼
                     [VoiceTtsFormatter]
                     (إزالة الماركداون والرموز)
                                │
                                ▼
                     [AudioFocusManager] (Ducking)
                                │
                                ▼
                     [PiperTtsManager]
                     (نطق الرد بصوت ONNX عالي الدقة)
```

---

## 5. مصفوفة الملفات والتعديلات (File Impact Matrix)

| الملف | نوع الإجراء | الوظيفة الأساسية |
| :--- | :--- | :--- |
| `app/build.gradle` | جديد/إنشاء | إعداد مشروع أندرويد نظيف مع Room و DataStore و Play Services Location و ONNX Runtime |
| `app/src/main/AndroidManifest.xml` | جديد | إضافة أذونات الصوت، الموقع، والخدمة الأمامية، وتسجيل AccessibilityService |
| `app/src/main/java/.../domain/` | جديد | نماذج الكيانات، واجهات المستودعات، وحالات الاستخدام النظيفة |
| `app/src/main/java/.../data/local/` | جديد | كيانات قاعدة بيانات Room، ومستودع DataStore |
| `app/src/main/java/.../data/ai/engine_a/` | جديد | موجه ONNX الدلالي الفوري لعتاد السيارة مع عتبة الـ 70% |
| `app/src/main/java/.../data/ai/engine_b/` | جديد | غلاف llama.cpp لنموذج GGUF مع حصر الأنوية في 2-3 threads |
| `app/src/main/java/.../data/car/` | جديد | طبقة إرسال أوامر عتاد السيارة ونوايا BYD DiLink |
| `app/src/main/java/.../data/mcp/` | جديد | عميل بروتوكول MCP، معالج JSON-RPC وأدوات الويب والشاشة |
| `app/src/main/java/.../accessibility/` | جديد | خدمة CarAccessibilityService لقراءة شاشة السيارة |
| `app/src/main/java/.../data/location/` | جديد | موفر الموقع الجغرافي والوقت اللحظي |
| `app/src/main/java/.../voice/` | جديد | Vosk لوضعيتي التنبيه والإملاء، ومعقم VoiceTtsFormatter و PiperTtsManager |
| `app/src/main/java/.../service/` | جديد | خدمة VoiceAssistantForegroundService الأمامية المستمرة |
| `app/src/main/java/.../presentation/` | جديد | واجهة MVVM واختيار نموذج GGUF ديناميكياً عبر منتقي الملفات |

---

## 6. خطة التحقق والاختبار (Verification & Test Plan)
1. **اختبارات الوحدة (Unit Tests):**
   * اختبار دقة وتطهير نصوص `VoiceTtsFormatterTest` والتأكد من إزالة الماركداون والرموز.
   * اختبار حاسبة جيب التمام وعتبة الـ 70% في `OnnxSemanticRouterTest`.
   * اختبار استعلام واسترجاع الكلمات المفتاحية في `MemoryDaoTest` باستخدام Room in-memory database.
   * اختبار توجيه أدوات MCP المدمجة في `McpToolRegistryTest`.
2. **اختبارات التكامل (Integration Tests):**
   * التحقق من تشغيل `VoiceAssistantForegroundService` وإدارة تركيز الصوت `AudioFocusManager`.
   * التحقق من استقرار استدعاءات `LlamaCppEngine` تحت قيود المعالج (2-3 threads).