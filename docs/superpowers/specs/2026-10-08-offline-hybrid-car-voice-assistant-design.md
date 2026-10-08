# وثيقة التصميم المعماري: المساعد الصوتي الهجين لشاشات سيارات BYD DiLink
## (Offline Hybrid Voice Assistant for Car Head Unit - Architecture Specification)

**التاريخ:** 08 أكتوبر 2026  
**الحالة:** معتمد من المستخدم (Approved)  
**المنصة المستهدفة:** BYD DiLink (نظام أندرويد 10+)  
**الهدف:** بناء مساعد صوتي أصلي متكامل فائق الاستقرار والأداء، يعمل دون الحاجة الدائمة للإنترنت، مع التوجيه الذكي الهجين (Hybrid Engine) بين أوامر العتاد السريعة والذكاء الاصطناعي التوليدي عبر بروتوكول MCP.

---

## 1. المعمارية العامة ومبادئ التصميم (Architecture Overview)

النظام مبني بلغة **Kotlin الأصلية بنسبة 100%**، مع الاستغناء التام عن بيئة Chaquopy/Python لتقليل حجم التطبيق وإلغاء استهلاك الذاكرة الإضافي وضمان سرعة الاستجابة اللحظية داخل السيارة.

### 1.1 مبادئ التصميم الأساسية
1. **Clean Architecture & Separation of Concerns:** فصل طبقات العرض والمنطق والبيانات والخدمات لضمان قابلية الاختبار والتوسعة.
2. **Resource Efficiency (Zero-Overhead Edge AI):** المحرك A يستجيب لأوامر السيارة الشائعة في أقل من 20 مللي ثانية دون إيقاظ الـ LLM؛ والمحرك B مقيّد بـ 2-3 أنوية CPU فقط لحماية معالج السيارة من السخونة والتجميد.
3. **Resilient Background Execution:** خدمة أمامية دائمة (`ForegroundService`) مصنفة بنوع `microphone` لضمان عدم إيقاف النظام للاستماع الصوتي أثناء تشغيل تطبيقات الملاحة أو الموسيقى في السيارة.
4. **Standardized Extensibility:** الاعتماد على بروتوكول سياق النماذج (Model Context Protocol - MCP) لإتاحة استدعاء الأدوات للـ LLM بدلاً من الحلول الترقيعية.

---

## 2. هيكل الحزم والطبقات (Package Structure)

```
com.ovos.arabicassistant/
├── presentation/                      # طبقة العرض وواجهة المستخدم (MVVM)
│   ├── viewmodel/                     # MainViewModel (StateFlow, SharedFlow)
│   ├── ui/                            # MainActivity, شاشة إعدادات النماذج، وعرض المحادثة
│   └── state/                         # AssistantUiState (Idle, Listening, Thinking, Speaking, Error)
├── domain/                            # طبقة الأعمال والمنطق المجرد (Pure Kotlin)
│   ├── model/                         # نماذج الأعمال:
│   │   ├── VoiceCommand.kt            # نصوص وأوامر المستخدم
│   │   ├── IntentResult.kt            # نتيجة التوجيه الدلالي ونسبة الثقة
│   │   ├── CarAction.kt               # أوامر عتاد السيارة (تكييف، نوافذ، وسائط)
│   │   ├── MemoryFact.kt              # حقائق الذاكرة المسترجعة
│   │   └── McpToolCall.kt             # استدعاءات أدوات MCP ونتائجها
│   ├── repository/                    # واجهات المستودعات:
│   │   ├── MemoryRepository.kt        # إدارة واسترجاع حقائق الذاكرة
│   │   ├── SettingsRepository.kt      # إدارة إعدادات النماذج والصوت
│   │   └── CarControlRepository.kt    # واجهة التحكم بعتاد السيارة
│   └── usecase/                       # حالات الاستخدام المستقلة:
│       ├── ProcessVoiceInputUseCase.kt     # المنسق الشامل بين المحركين A و B
│       ├── RouteSemanticIntentUseCase.kt   # التوجيه الدلالي عبر المحرك A
│       ├── ExecuteCarActionUseCase.kt      # تنفيذ أمر عتاد السيارة
│       ├── GenerateLlmResponseUseCase.kt   # توليد الرد عبر المحرك B مع حقن السياق
│       ├── QueryMcpToolsUseCase.kt         # معالجة أدوات MCP
│       └── RetrieveMemoryFactsUseCase.kt   # استرجاع سياق الذاكرة RAG
├── data/                              # طبقة البيانات والعتاد والمصادر الخارجية
│   ├── local/                         # التخزين المحلي
│   │   ├── db/                        # قاعدة بيانات Room (الذاكرة RAG)
│   │   │   ├── AppDatabase.kt
│   │   │   ├── MemoryEntity.kt
│   │   │   └── MemoryDao.kt           # استعلام الكلمات المفتاحية بالـ FTS أو SQL LIKE
│   │   └── preferences/               # Jetpack DataStore
│   │       └── SettingsDataStore.kt   # مسار نموذج GGUF، عدد الأنوية، سرعة النطق
│   ├── ai/                            # محركات الذكاء الاصطناعي على الحافة
│   │   ├── engine_a/                  # المحرك السريع: ONNX Runtime
│   │   │   ├── OnnxSemanticRouter.kt  # استدلال نموذج MiniLM 384D
│   │   │   └── IntentAnchorRegistry.kt# مصفوفة المتجهات المسبقة لأوامر السيارة
│   │   └── engine_b/                  # المحرك التوليدي: llama.cpp
│   │       ├── LlamaCppEngine.kt      # غلاف JNI لتشغيل نماذج .gguf
│   │       └── LlamaConfig.kt         # ضبط خيوط المعالجة (2-3 Threads) ودرجة الحرارة
│   ├── car/                           # طبقة التحكم بعتاد السيارة
│   │   ├── CarControlManager.kt       # الموزع الرئيسي لأوامر العتاد
│   │   ├── BydCarIntentDispatcher.kt  # إرسال نوايا BYD ونوايا أندرويد القياسية
│   │   └── SystemMediaController.kt   # التحكم المباشر بمستوى الصوت والوسائط
│   ├── location/                      # الموقع والوقت الحقيقي
│   │   └── LocationTimeProvider.kt    # FusedLocationProviderClient + Geocoder + System Clock
│   └── mcp/                           # عميل بروتوكول سياق النماذج (MCP Layer)
│       ├── McpClient.kt               # معالج بروتوكول JSON-RPC 2.0
│       ├── McpToolRegistry.kt         # تسجيل الأدوات واستدعاؤها
│       ├── tools/                     # الأدوات المدمجة
│       │   ├── WebSearchTool.kt       # استعلام الويب الحي (Open-Meteo, DuckDuckGo)
│       │   └── ScreenAwarenessTool.kt # استخراج عناصر واجهة الشاشة
│       └── transport/                 # ناقل البروتوكول (In-Memory محلي + OkHttp SSE)
├── service/                           # خدمات أندرويد الخلفية
│   ├── VoiceAssistantForegroundService.kt  # الخدمة الأمامية لإدارة دورة حياة الميكروفون
│   └── notification/                       # إشعارات الخدمة الأمامية
├── accessibility/                     # خدمة إمكانية الوصول
│   └── CarAccessibilityService.kt     # فحص شجرة الـ UI Nodes وتزويد سياق الشاشة
└── voice/                             # معالجة الصوت والنطق
    ├── VoskSpeechManager.kt           # آلة حالات Vosk (كلمة التنبيه + التقاط الجملة)
    ├── PiperTtsManager.kt             # محرك Piper ONNX مع التراجع لمحرك النظام
    ├── VoiceTtsFormatter.kt           # تعقيم وتطهير نصوص الـ LLM قبل نطقها
    └── AudioFocusManager.kt           # خفض صوت راديو ووسائط السيارة (Audio Ducking)
```

---

## 3. تفاصيل المكونات الرئيسية

### 3.1 خط أنابيب الصوت وخدمة الاستماع الدائمة (Audio Pipeline & Foreground Service)
* **`VoiceAssistantForegroundService`:**
  * تُسجّل في `AndroidManifest.xml` كخدمة بنوع `microphone`.
  * تمنع نظام أندرويد على شاشة السيارة من قتل التطبيق في الخلفية عند فتح تطبيق الملاحة أو الخرائط.
  * إشعار دائم غير مزعج في شريط إشعارات السيارة يوضح حالة المساعد.
* **إدارة تركيز الصوت (`AudioFocusManager`):**
  * طلب تركيز صوتي بنوع `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
  * عند التقاط كلمة التنبيه أو أثناء تشغيل TTS، يُخفض صوت وسائط السيارة تلقائياً إلى 20%، ويُستعاد فور انتهاء المساعد من الحديث.
* **آلة حالات Vosk ثنائية المراحل (`VoskSpeechManager`):**
  * **المرحلة 1 (Wake Word Mode):** يعمل المحرك مع قاموس محصور بكلمات التنبيه المحددة (مثل: `"يا سيارة"`، `"مساعد السيارة"`). استهلاك المعالج يقل عن 2%.
  * **المرحلة 2 (Full Dictation Mode):** عند رصد كلمة التنبيه، يُصدر التطبيق صوت تنبيه قصير، وينتقل Vosk فوراً إلى وضع التعرف المفتوح لالتقاط جملة السائق بالكامل.
  * **كشف نهاية الكلام (VAD / EOS):** عند مرور 700ms من الصمت، تُغلق الجملة وتُرسل مباشرة إلى `ProcessVoiceInputUseCase`، ويعود المحرك لوضع الاستماع لكلمة التنبيه.

---

### 3.2 العقل الهجين: المحرك A والمحرك B (Hybrid Brain Routing)
* **المحرك A (`OnnxSemanticRouter`):**
  * يعتمد على `com.microsoft.onnxruntime:onnxruntime-android`.
  * يقوم بتحويل نص المستخدم إلى متجه تضمين 384D، ثم حساب تشابه جيب التمام مع أنماط عتاد السيارة المعتمدة في `intent_embeddings.json`.
  * **قاعدة العتبة الصارمة ($\ge 0.70$):**
    * إذا كانت النتيجة $\ge 0.70$: يُنفذ أمر السيارة المادي فوراً ويتم الرد بعبارة تأكيدية قصيرة (زمن التنفيذ الكلي $< 25\text{ ms}$).
    * إذا كانت النتيجة $< 0.70$: يتم تمرير الأمر تلقائياً إلى المحرك B.
* **المحرك B (`LlamaCppEngine`):**
  * غلاف JNI لنواة `llama.cpp` يدعم نماذج `.gguf` بصيغ تكميم فعالة (Q4_K_M أو Q4_0).
  * **حماية موارد السيارة:** حصر خيوط المعالجة في 2 أو 3 أنوية كحد أقصى لمنع استنزاف موارد وحدة المعالجة المركزية لشاشة BYD DiLink.
  * تدفق الرموز المولدة لحظياً (Streaming via Kotlin `Flow<String>`).

---

### 3.3 طبقة التحكم بعتاد السيارة (Car Hardware Control Layer)
* **الوسائط والصوت:**
  * ضبط درجات الصوت عبر `AudioManager`.
  * إرسال نوايا تحكم بالوسائط: `android.intent.action.MEDIA_BUTTON` مع `KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_MEDIA_NEXT`, `KEYCODE_MEDIA_PREVIOUS`.
* **التكييف والنوافذ (BYD DiLink Bridge):**
  * إرسال نوايا بث موجهة لسيارات BYD (مثل: نوايا تعديل الحرارة، مروحة التكييف، فتح النوافذ).
  * تراجع آمن في حال عدم استجابة العتاد بنطق تنبيه واضح للسائق: "تم إرسال الأمر لنظام السيارة".

---

### 3.4 عميل بروتوكول سياق النماذج (MCP Client Layer)
* **المعمارية:**
  * متوافق مع معيار **Model Context Protocol (JSON-RPC 2.0)**.
  * يدعم وضعين: **In-App Direct Tools** لأدوات أندرويد المحلية، و **Remote SSE** لأي خوادم خارجية متصلة.
* **أداة البحث المباشر على الويب (`WebSearchTool`):**
  * المدخلات: استعلام البحث `query`.
  * المعالجة: استدعاء واجهات بحث خفيفة وسريعة (DuckDuckGo Instant Answer / Open-Meteo API) عبر عميل `OkHttp`.
  * المخرجات: نص موجز ودقيق يُغذى به الـ LLM لإعطاء إجابة محدثة للسائق.
* **أداة الوعي بمحتوى الشاشة (`ScreenAwarenessTool`):**
  * الاتصال بـ `CarAccessibilityService`.
  * استخراج النصوص والأزرار البارزة من شجرة الـ `AccessibilityNodeInfo`.
  * توفير سياق الشاشة للـ LLM للإجابة على تساؤلات مثل: "اقرأ ما هو مكتوب على الشاشة"، "من المتصل الآن؟".

---

### 3.5 السياق الديناميكي والذاكرة طويلة المدى (Dynamic Context & Room RAG)
* **حقن الموقع والوقت (`LocationTimeProvider`):**
  * استخدام `FusedLocationProviderClient` مع صلاحية `ACCESS_FINE_LOCATION`.
  * حقن الإحداثيات واسم المدينة والتاريخ والوقت تلقائياً في مقدمة الـ System Prompt عند طلبات الطقس والوقت والملاحة.
* **الذاكرة الخفيفة (`Room DB RAG`):**
  * جدول `memory_facts`: يخزن حقائق تفضيلات السائق والمعلومات المحفوظة.
  * استرجاع أهم 2-3 حقائق فقط مطابقة للكلمات المفتاحية في جملة المستخدم، لحماية نافذة السياق الصغيرة للـ LLM وتوفير ذاكرة الوصول العشوائي (RAM).

---

### 3.6 معقم النصوص الصوتي ومحرك النطق (Voice-First TTS Formatter & Piper TTS)
* **تعقيم النصوص (`VoiceTtsFormatter`):**
  * إزالة رموز الماركداون: `**`, `*`, `###`, `_`, `[ ]`, `( )`.
  * حذف الرموز التعبيرية (Emojis) والرسوم التعبيرية.
  * تحويل الرموز الشائعة لكلمات صريحة: `%` $\rightarrow$ "بالمئة"، `+` $\rightarrow$ "زائد"، `&` $\rightarrow$ "و".
  * استبدال `\n` و `\r` بمسافات فردية لضمان نطق سليم ومتصل.
* **النطق الصوتي (`PiperTtsManager`):**
  * تشغيل نماذج Piper ONNX الصوتية العالية الدقة.
  * ضبط وتيرة النطق (`length_scale = 1.15`) لصوت هادئ ومناسب لبيئة قيادة السيارة.
  * تراجع تلقائي إلى محرك أندرويد المدمج `TextToSpeech` في حال عدم وجود ملفات الموديل.

---

### 3.7 واجهة المستخدم واختيار النموذج ديناميكياً (Presentation & Dynamic Model Selection)
* **شاشة التطبيق الرئيسية (`MainActivity`):**
  * عرض حالة المساعد اللحظية (Listening / Processing / Speaking / Idle).
  * خيار تصفح واختيار أي نموذج بصيغة `.gguf` من ذاكرة التخزين عبر `ActivityResultContracts.GetContent()`.
  * حفظ مسار النموذج في `DataStore` مع الحصول على صلاحية دائمة للمسار (`takePersistableUriPermission`).
  * لوحة إعدادات لضبط عدد الأنوية (2 أو 3 Threads) وسرعة نطق الصوت.

---

## 4. مصفوفة تعديل وإنشاء الملفات (File Impact Matrix)

| المسار | نوع الإجراء | الوظيفة |
| :--- | :--- | :--- |
| `app/build.gradle` | تعديل | إزالة Chaquopy، إضافة Room و DataStore و Play Services Location و NDK |
| `app/src/main/AndroidManifest.xml` | تعديل | إضافة أذونات الموقع والخدمة الأمامية للميكروفون وخدمة الـ Accessibility |
| `app/src/main/java/com/ovos/arabicassistant/presentation/` | جديد | بنية MVVM، ViewModel، وحالات الواجهة واختيار النموذج |
| `app/src/main/java/com/ovos/arabicassistant/domain/` | جديد | نماذج النطاق النظيفة وحالات الاستخدام (Use Cases) |
| `app/src/main/java/com/ovos/arabicassistant/data/local/db/` | جديد | قاعدة بيانات Room، كيان الذاكرة وواجهة DAO للـ RAG |
| `app/src/main/java/com/ovos/arabicassistant/data/local/preferences/` | جديد | Jetpack DataStore لحفظ مسارات النماذج والإعدادات |
| `app/src/main/java/com/ovos/arabicassistant/data/ai/engine_a/` | جديد | OnnxSemanticRouter المستقل بلغة Kotlin الصافية مع عتبة 70% |
| `app/src/main/java/com/ovos/arabicassistant/data/ai/engine_b/` | جديد | LlamaCppEngine غلاف JNI لتشغيل نماذج GGUF مع حصر الأنوية |
| `app/src/main/java/com/ovos/arabicassistant/data/car/` | جديد | طبقة التحكم المادي بعتاد سيارة BYD ونوايا الوسائط |
| `app/src/main/java/com/ovos/arabicassistant/data/location/` | جديد | LocationTimeProvider لجلب GPS والوقت الفعلي |
| `app/src/main/java/com/ovos/arabicassistant/data/mcp/` | جديد | عميل بروتوكول MCP، مع تسجيل أدوات الويب والشاشة |
| `app/src/main/java/com/ovos/arabicassistant/service/` | جديد | VoiceAssistantForegroundService لإدارة الميكروفون بالخلفية |
| `app/src/main/java/com/ovos/arabicassistant/accessibility/` | جديد | CarAccessibilityService لقراءة محتوى شاشة السيارة |
| `app/src/main/java/com/ovos/arabicassistant/voice/VoiceTtsFormatter.kt` | جديد | تعقيم صارم للنصوص قبل إرسالها لـ Piper TTS |
| `app/src/main/java/com/ovos/arabicassistant/voice/VoskSpeechManager.kt` | جديد | التبديل بين كاشف كلمة التنبيه والتعرف على كامل الجملة |

---

## 5. خطة التحقق والاختبار (Verification Plan)

1. **اختبارات الوحدة (Unit Tests):**
   * اختبار `VoiceTtsFormatterTest`: التحقق من تجريد الماركداون والرموز التعبيرية واستبدال الرموز الحسابية.
   * اختبار `OnnxSemanticRouterTest`: التحقق من أن الأوامر ذات الثقة $\ge 0.70$ تُوجه للعتاد، وما دونها يُوجه للـ LLM.
   * اختبار `MemoryDaoTest`: التحقق من حفظ واسترجاع حقائق الذاكرة بالكلمات المفتاحية.
   * اختبار `McpToolRegistryTest`: التحقق من عمل أدوات البحث وسياق الشاشة عبر رسائل JSON-RPC.
2. **اختبارات التكامل والبناء (Build Verification):**
   * التأكد من بناء المشروع بنجاح دون أخطاء بعد استبعاد Chaquopy عبر `./gradlew assembleDebug` أو فحص التبعيات وتوافق الـ NDK.
   * التأكد من استجابة الخدمة الأمامية لإشعارات الميكروفون وتكامل خدمة الـ Accessibility.
