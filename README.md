# BYD DiLink Offline Hybrid Voice Assistant (Android 10+)
[![Build & Release Android APK](https://github.com/Ahmed69450/byd-offline-voice-assistant/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Ahmed69450/byd-offline-voice-assistant/actions/workflows/build-apk.yml)

مساعد صوتي ذكي هجين محلي الصنع بالكامل (100% Native Kotlin)، مصمم ومُحسّن خصيصاً لشاشات سيارات **BYD DiLink** وأنظمة السيارات الذكية (Android 10+).

---

## 🚀 المعمارية والمميزات الرئيسية (Core Features)

1. **الاستماع المستمر وكلمة التنبيه (Continuous Audio Pipeline):**
   * خدمة أمامية دائمة (`VoiceAssistantForegroundService`) بنوع `microphone`.
   * محرك **Vosk Offline** ثنائي المراحل: رصد كلمة التنبيه خفيف الموارد (`"يا سيارة"`, `"مرحبا سيارة"`) باستهلاك < 2% من المعالج، ثم الانتقال التلقائي للتعرف الصوتي الكامل (Full ASR) مع كاشف الصمت (VAD 700ms).
   * خفض صوت وسائط وراديو السيارة تلقائياً (`AudioFocusManager` مع Audio Ducking).

2. **العقل الهجين ثنائي المحركات (Dual-Engine Brain):**
   * **المحرك A (الموجه الدلالي السريع - Engine A):** استدلال محلي فوري عبر `onnxruntime-android` بنموذج MiniLM (384D) لتصنيف وتنفيذ أوامر عتاد السيارة في أقل من **20 مللي ثانية** عند تحقق نسبة ثقة $\ge 70\%$.
   * **المحرك B (النموذج اللغوي الاحتياطي - Engine B):** غلاف JNI لـ `llama.cpp` لتشغيل نماذج `.gguf` الخفيفة محلياً، مقيّد بـ **2 إلى 3 أنوية CPU فقط** لحماية معالج شاشة السيارة من السخونة واستهلاك الذاكرة، مع تدفق لحظي للرموز (`Flow<String>`).

3. **طبقة عتاد السيارة (Car Hardware Control Layer):**
   * تحكم مباشر في التكييف (درجات الحرارة، سرعة المروحة)، النوافذ، فتحة السقف، ومستوى الصوت والوسائط عبر نوايا بث BYD DiLink وأندرويد.

4. **بروتوكول سياق النماذج (Model Context Protocol - MCP Client):**
   * عميل JSON-RPC 2.0 متوافق مع مواصفات MCP القياسية للأدوات المدمجة والبعيدة:
     * **البحث المباشر في الويب (`WebSearchTool`):** جلب الأخبار والأحداث الحية عبر الإنترنت تلقائياً.
     * **الوعي بمحتوى الشاشة (`ScreenAwarenessTool`):** قراءة عناصر واجهة المستخدم والتطبيق النشط حالياً عبر `CarAccessibilityService`.

5. **السياق اللحظي والذاكرة طويلة المدى (Dynamic Context & Memory):**
   * تتبع الموقع الجغرافي GPS الدقيق واسم المدينة عبر `FusedLocationProviderClient` وحقن الوقت والتاريخ الفعلي.
   * ذاكرة طويلة المدى موفرة عبر **Room Database** لاسترجاع تفضيلات السائق بالكلمات المفتاحية للجولة الحالية فقط (Lightweight RAG).

6. **مُعقّم النطق واختيار النماذج ديناميكياً:**
   * أداة `VoiceTtsFormatter` لتطهير نصوص الـ LLM من الماركداون والرموز التعبيرية وتحويل الرموز الحسابية لكلمات عربية صريحة لمحرك **Piper TTS**.
   * شاشة إعدادات لاختيار ملفات `.gguf` مباشرة من ذاكرة السيارة وضبط عدد الأنوية.

---

## 🛠️ متطلبات البناء (Build Requirements)
* **Android SDK:** 34 (minSdk 24)
* **JDK:** OpenJDK 17
* **Gradle:** 8.5+
* **اللغة:** Kotlin 1.9+

---

## 📦 تنزيل حزمة التطبيق (Download APK)
يمكنك تنزيل أحدث إصدار من حزمة التطبيق **(v1.0.0 APK)** مباشرة من قسم [Releases](https://github.com/Ahmed69450/byd-offline-voice-assistant/releases).
