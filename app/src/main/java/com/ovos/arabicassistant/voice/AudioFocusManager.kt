package com.ovos.arabicassistant.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * مدير تركيز الصوت لبيئة سيارات BYD DiLink
 * يقوم بخفض صوت الوسائط والراديو (Audio Ducking) أثناء استماع أو حديث المساعد،
 * وإعادة الصوت لحالته الطبيعية بمجرد انتهاء التفاعل الصوتي.
 */
class AudioFocusManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false

    companion object {
        private const val TAG = "AudioFocusManager"
    }

    /**
     * طلب خفض صوت وسائط السيارة أثناء التفاعل الصوتي
     */
    fun requestDucking(): Boolean {
        if (audioManager == null) return false
        if (hasFocus) return true

        return try {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "تغيرت حالة تركيز الصوت: $focusChange")
                    }
                    .build()

                focusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }

            hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            hasFocus
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء طلب تركيز الصوت: ${e.message}")
            false
        }
    }

    /**
     * التخلي عن تركيز الصوت وإعادة صوت وسائط السيارة لوضعه الطبيعي
     */
    fun abandonFocus() {
        if (audioManager == null || !hasFocus) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            hasFocus = false
        } catch (e: Exception) {
            Log.e(TAG, "خطأ أثناء التخلي عن تركيز الصوت: ${e.message}")
        }
    }
}
