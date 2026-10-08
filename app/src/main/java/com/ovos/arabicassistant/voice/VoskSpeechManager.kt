package com.ovos.arabicassistant.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import kotlin.math.sqrt

enum class VoskSpeechState {
    IDLE,
    WAKE_WORD_LISTENING,
    ACTIVE_DICTATION,
    PROCESSING
}

/**
 * مدير التعرف الصوتي ثنائي المراحل باستخدام محرك Vosk Offline
 * المرحلة 1: رصد كلمة التنبيه ("يا سيارة") باستهلاك ضئيل للمعالج (<2%).
 * المرحلة 2: التقاط الجملة الكاملة مع كاشف الصمت (VAD 700ms) لتحديد نهاية الكلام.
 */
class VoskSpeechManager(
    private val context: Context,
    private val listener: VoskEventListener
) {

    interface VoskEventListener {
        fun onReady()
        fun onWakeWordTriggered()
        fun onPartialSpeech(partial: String)
        fun onCommandCaptured(utterance: String)
        fun onError(error: String)
        fun onStateChanged(state: VoskSpeechState)
    }

    companion object {
        private const val TAG = "VoskSpeechManager"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SILENCE_TIMEOUT_MS = 750L
        private const val SILENCE_ENERGY_THRESHOLD = 350.0

        val WAKE_WORDS = listOf("يا سيارة", "مرحبا سيارة", "مساعد سيارة")
        const val WAKE_GRAMMAR = "[\"يا سيارة\", \"مرحبا سيارة\", \"مساعد سيارة\", \"[unk]\"]"
    }

    private var model: Model? = null
    private var wakeRecognizer: Recognizer? = null
    private var fullRecognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null

    var currentState = VoskSpeechState.IDLE
        private set

    private var isRecording = false
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initialize(onComplete: (Boolean) -> Unit) {
        scope.launch {
            try {
                val modelDir = File(context.filesDir, "vosk-model-ar")
                if (modelDir.exists()) {
                    model = Model(modelDir.absolutePath)
                    wakeRecognizer = Recognizer(model, SAMPLE_RATE.toFloat(), WAKE_GRAMMAR)
                    fullRecognizer = Recognizer(model, SAMPLE_RATE.toFloat())
                    withContext(Dispatchers.Main) {
                        listener.onReady()
                        onComplete(true)
                    }
                } else {
                    Log.w(TAG, "نموذج Vosk غير متوفر في التخزين الداخلي: ${modelDir.absolutePath}")
                    withContext(Dispatchers.Main) {
                        onComplete(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "فشلت تهيئة محرك Vosk: ${e.message}")
                withContext(Dispatchers.Main) {
                    listener.onError(e.message ?: "خطأ في تهيئة Vosk")
                    onComplete(false)
                }
            }
        }
    }

    fun startListening() {
        if (isRecording) return
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                listener.onError("تعذر تهيئة الميكروفون للخدمة المستمرة.")
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            transitionState(VoskSpeechState.WAKE_WORD_LISTENING)

            recordingJob = scope.launch {
                val buffer = ShortArray(bufferSize / 2)
                var lastVoiceTime = System.currentTimeMillis()
                var hadVoiceInDictation = false

                while (isActive && isRecording) {
                    val readSamples = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readSamples <= 0) continue

                    val energy = calculateEnergy(buffer, readSamples)
                    val now = System.currentTimeMillis()

                    if (energy > SILENCE_ENERGY_THRESHOLD) {
                        lastVoiceTime = now
                        if (currentState == VoskSpeechState.ACTIVE_DICTATION) {
                            hadVoiceInDictation = true
                        }
                    }

                    when (currentState) {
                        VoskSpeechState.WAKE_WORD_LISTENING -> {
                            val activeRec = wakeRecognizer ?: fullRecognizer
                            if (activeRec != null && activeRec.acceptWaveForm(buffer, readSamples)) {
                                val result = parseText(activeRec.result)
                                if (WAKE_WORDS.any { result.contains(it) }) {
                                    handleWakeWordTrigger()
                                    lastVoiceTime = now
                                    hadVoiceInDictation = false
                                }
                            }
                        }

                        VoskSpeechState.ACTIVE_DICTATION -> {
                            val activeRec = fullRecognizer
                            if (activeRec != null) {
                                if (activeRec.acceptWaveForm(buffer, readSamples)) {
                                    val text = parseText(activeRec.result)
                                    if (text.isNotBlank()) {
                                        handleCommandCaptured(text)
                                    }
                                } else {
                                    val partial = parsePartial(activeRec.partialResult)
                                    if (partial.isNotBlank()) {
                                        withContext(Dispatchers.Main) {
                                            listener.onPartialSpeech(partial)
                                        }
                                    }
                                }

                                // فحص انتهاء الكلام والصمت (VAD)
                                if (hadVoiceInDictation && (now - lastVoiceTime) > SILENCE_TIMEOUT_MS) {
                                    val finalResult = parseText(activeRec.finalResult)
                                    handleCommandCaptured(finalResult)
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            listener.onError("خطأ في تشغيل الاستماع الصوتي: ${e.message}")
            stopListening()
        }
    }

    private suspend fun handleWakeWordTrigger() {
        fullRecognizer?.reset()
        transitionState(VoskSpeechState.ACTIVE_DICTATION)
        withContext(Dispatchers.Main) {
            listener.onWakeWordTriggered()
        }
    }

    private suspend fun handleCommandCaptured(command: String) {
        transitionState(VoskSpeechState.PROCESSING)
        withContext(Dispatchers.Main) {
            listener.onCommandCaptured(command)
        }
        // العودة التلقائية لوضع رصد كلمة التنبيه
        fullRecognizer?.reset()
        wakeRecognizer?.reset()
        transitionState(VoskSpeechState.WAKE_WORD_LISTENING)
    }

    private fun transitionState(newState: VoskSpeechState) {
        currentState = newState
        scope.launch(Dispatchers.Main) {
            listener.onStateChanged(newState)
        }
    }

    private fun parseText(jsonString: String): String {
        return try {
            JSONObject(jsonString).optString("text", "").trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun parsePartial(jsonString: String): String {
        return try {
            JSONObject(jsonString).optString("partial", "").trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun calculateEnergy(buffer: ShortArray, size: Int): Double {
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        return sqrt(sum / size)
    }

    fun stopListening() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء إيقاف AudioRecord: ${e.message}")
        }
        audioRecord = null
        transitionState(VoskSpeechState.IDLE)
    }
}
