package com.ovos.arabicassistant.data.ai.engine_b

import android.util.Log

/**
 * جسر الربط مع مكتبة llama.cpp الأصلية عبر JNI/C++
 * يتولى تحميل نماذج GGUF وتوليد الرموز النصية بتدفق لحظي.
 */
open class LlamaCppBridge {

    companion object {
        private const val TAG = "LlamaCppBridge"
        private var isNativeLibLoaded = false

        init {
            try {
                System.loadLibrary("llama-android")
                isNativeLibLoaded = true
                Log.i(TAG, "تم تحميل مكتبة llama-android JNI بنجاح.")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "مكتبة llama-android الأصلية غير محملة، سيتم تفعيل وضع المحاكاة: ${e.message}")
                isNativeLibLoaded = false
            }
        }
    }

    open fun loadModel(modelPath: String, threads: Int): Boolean {
        return if (isNativeLibLoaded) {
            try {
                nativeLoadModel(modelPath, threads)
            } catch (e: Exception) {
                Log.e(TAG, "خطأ JNI أثناء تحميل النموذج: ${e.message}")
                false
            }
        } else {
            Log.d(TAG, "[محاكاة] تم تحميل نموذج GGUF من: $modelPath بـ $threads أنوية")
            true
        }
    }

    open fun generate(prompt: String, onToken: (String) -> Unit): Boolean {
        return if (isNativeLibLoaded) {
            try {
                nativeGenerate(prompt, onToken)
            } catch (e: Exception) {
                Log.e(TAG, "خطأ JNI أثناء التوليد: ${e.message}")
                false
            }
        } else {
            // محاكاة استجابة ذكية عند عدم توفر المكتبة الأصلية أثناء التطوير
            onToken("أهلاً بك! ")
            onToken("أنا مساعد BYD الذكي، ")
            onToken("جاهز لمساعدتك في قيادتك.")
            true
        }
    }

    open fun stop() {
        if (isNativeLibLoaded) {
            nativeStop()
        }
    }

    open fun freeModel() {
        if (isNativeLibLoaded) {
            nativeFreeModel()
        }
    }

    // دوال JNI الأصلية
    private external fun nativeLoadModel(path: String, threads: Int): Boolean
    private external fun nativeGenerate(prompt: String, callback: Any): Boolean
    private external fun nativeStop()
    private external fun nativeFreeModel()
}
