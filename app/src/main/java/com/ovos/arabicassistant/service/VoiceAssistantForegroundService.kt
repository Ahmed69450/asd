package com.ovos.arabicassistant.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.ovos.arabicassistant.service.ServiceNotificationManager.Companion.NOTIFICATION_ID
import com.ovos.arabicassistant.voice.VoskSpeechManager
import com.ovos.arabicassistant.voice.VoskSpeechState

/**
 * الخدمة الأمامية المستمرة للسيارة (VoiceAssistantForegroundService)
 * مسجلة بنوع foregroundServiceType="microphone" لحماية استماع المساعد
 * من إغلاق نظام أندرويد 10+ أثناء القيادة.
 */
class VoiceAssistantForegroundService : Service() {

    companion object {
        private const val TAG = "AssistantService"
        const val ACTION_START_LISTENING = "com.ovos.arabicassistant.action.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.ovos.arabicassistant.action.STOP_LISTENING"
    }

    private lateinit var notificationManager: ServiceNotificationManager
    private var voskManager: VoskSpeechManager? = null
    private val binder = LocalBinder()

    var onWakeWordListener: (() -> Unit)? = null
    var onCommandCapturedListener: ((String) -> Unit)? = null
    var onStateChangedListener: ((VoskSpeechState) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): VoiceAssistantForegroundService = this@VoiceAssistantForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = ServiceNotificationManager(this)
        startForeground(NOTIFICATION_ID, notificationManager.buildNotification())

        setupVosk()
        Log.i(TAG, "تم بدء الخدمة الأمامية لمساعد السيارة بنجاح.")
    }

    private fun setupVosk() {
        voskManager = VoskSpeechManager(this, object : VoskSpeechManager.VoskEventListener {
            override fun onReady() {
                voskManager?.startListening()
            }

            override fun onWakeWordTriggered() {
                Log.i(TAG, "تم رصد كلمة التنبيه (Wake Word)")
                onWakeWordListener?.invoke()
            }

            override fun onPartialSpeech(partial: String) {
                // نص لحظي
            }

            override fun onCommandCaptured(utterance: String) {
                Log.i(TAG, "تم التقاط الأمر الصوتي: $utterance")
                onCommandCapturedListener?.invoke(utterance)
            }

            override fun onError(error: String) {
                Log.e(TAG, "خطأ في محرك Vosk: $error")
            }

            override fun onStateChanged(state: VoskSpeechState) {
                onStateChangedListener?.invoke(state)
            }
        })
        voskManager?.initialize { success ->
            if (success) {
                Log.i(TAG, "محرك Vosk جاهز وبدأ الاستماع لكلمة التنبيه.")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> voskManager?.startListening()
            ACTION_STOP_LISTENING -> voskManager?.stopListening()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        voskManager?.stopListening()
        Log.i(TAG, "تم إيقاف الخدمة الأمامية لمساعد السيارة.")
    }
}
