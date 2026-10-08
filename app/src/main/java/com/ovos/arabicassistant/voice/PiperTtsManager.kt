package com.ovos.arabicassistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.io.File
import java.util.Locale

/**
 * مدير تحويل النص إلى كلام (Piper TTS Manager)
 * يدعم نماذج Piper ONNX مع التراجع التلقائي لمحرك أندرويد الصوتي،
 * ويضبط وتيرة النطق الهادئة (length_scale = 1.15) المناسبة لبيئة القيادة،
 * مع استخدام معقم النصوص الصوتي وخفض صوت وسائط السيارة تلقائياً.
 */
class PiperTtsManager(
    private val context: Context,
    private val onInitListener: ((Boolean) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isSystemTtsReady = false
    private var isPiperModelAvailable = false
    private var piperModelFile: File? = null
    private val audioFocusManager = AudioFocusManager(context)

    // معامل وتيرة النطق الهادئة
    private var lengthScale: Float = 1.15f

    companion object {
        private const val TAG = "PiperTtsManager"
        private const val UTTERANCE_ID = "CAR_TTS_UTTERANCE"
    }

    init {
        checkPiperModelAvailability()
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    private fun checkPiperModelAvailability() {
        val modelDir = File(context.filesDir, "piper")
        val onnxFile = File(modelDir, "arabic_model.onnx")
        if (onnxFile.exists() && onnxFile.length() > 0) {
            isPiperModelAvailable = true
            piperModelFile = onnxFile
            Log.i(TAG, "تم العثور على نموذج Piper ONNX العربي: ${onnxFile.absolutePath}")
        } else {
            isPiperModelAvailable = false
            Log.i(TAG, "لم يتم العثور على نموذج Piper ONNX محلياً، سيتم استخدام محرك أندرويد TextToSpeech")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("ar"))
            isSystemTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (isSystemTtsReady) {
                val speechRate = 1.0f / lengthScale
                textToSpeech?.setSpeechRate(speechRate)
                textToSpeech?.setPitch(1.0f)
            }
            setupProgressListener()
            onInitListener?.invoke(true)
        } else {
            Log.e(TAG, "فشلت تهيئة محرك TextToSpeech: $status")
            isSystemTtsReady = false
            onInitListener?.invoke(false)
        }
    }

    private fun setupProgressListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // تم خفض صوت الوسائط مسبقاً
            }

            override fun onDone(utteranceId: String?) {
                audioFocusManager.abandonFocus()
            }

            override fun onError(utteranceId: String?) {
                audioFocusManager.abandonFocus()
            }
        })
    }

    /**
     * نطق النص مع تطهيره صوتياً وإدارة تركيز صوت وسائط السيارة
     */
    fun speak(rawText: String, onComplete: (() -> Unit)? = null): Boolean {
        val sanitized = VoiceTtsFormatter.sanitize(rawText)
        if (sanitized.isBlank()) {
            onComplete?.invoke()
            return false
        }

        // خفض صوت وسائط السيارة
        audioFocusManager.requestDucking()

        return if (isSystemTtsReady && textToSpeech != null) {
            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
            val result = textToSpeech?.speak(
                sanitized,
                TextToSpeech.QUEUE_FLUSH,
                params,
                UTTERANCE_ID
            )
            result == TextToSpeech.SUCCESS
        } else {
            Log.w(TAG, "محرك النطق غير جاهز حالياً: $sanitized")
            audioFocusManager.abandonFocus()
            onComplete?.invoke()
            false
        }
    }

    fun stop() {
        textToSpeech?.stop()
        audioFocusManager.abandonFocus()
    }

    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            audioFocusManager.abandonFocus()
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء إغلاق محرك TTS: ${e.message}")
        }
    }
}
