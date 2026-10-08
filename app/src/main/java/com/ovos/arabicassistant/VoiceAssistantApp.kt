package com.ovos.arabicassistant

import android.app.Application
import android.util.Log

/**
 * فئة التطبيق الرئيسية (VoiceAssistantApp)
 * مسؤولة عن دورة حياة التطبيق على شاشة سيارة BYD DiLink.
 */
class VoiceAssistantApp : Application() {

    companion object {
        const val TAG = "VoiceAssistantApp"
        lateinit var instance: VoiceAssistantApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "تم بدء تشغيل تطبيق المساعد الصوتي الهجين للسيارة بنجاح (100% Native Kotlin)")
    }
}
