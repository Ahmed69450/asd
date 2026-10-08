package com.ovos.arabicassistant.data.car

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent

/**
 * مرسل نوايا عتاد السيارة (BYD DiLink & Android System Intents)
 * ينفذ التحكم في التكييف، النوافذ، فتحة السقف، ومستوى الصوت والوسائط.
 */
open class BydCarIntentDispatcher(protected val context: Context?) {

    companion object {
        private const val TAG = "BydCarIntentDispatcher"

        // نوايا BYD DiLink المخصصة لعتاد السيارة
        const val BYD_ACTION_AC_TEMP = "byd.intent.action.AC_TEMP"
        const val BYD_ACTION_AC_FAN = "byd.intent.action.AC_FAN"
        const val BYD_ACTION_WINDOW = "byd.intent.action.WINDOW_CONTROL"
        const val BYD_ACTION_SUNROOF = "byd.intent.action.SUNROOF_CONTROL"

        const val EXTRA_TEMP_DELTA = "temp_delta"
        const val EXTRA_FAN_SPEED = "fan_speed"
        const val EXTRA_WINDOW_STATE = "window_state"
        const val EXTRA_SUNROOF_STATE = "sunroof_state"
    }

    open fun dispatchAcCommand(tempDelta: Int): Boolean {
        if (context == null) {
            Log.d(TAG, "[محاكاة] تعديل درجة حرارة المكيف بمقدار: $tempDelta")
            return true
        }

        return try {
            val intent = Intent(BYD_ACTION_AC_TEMP).apply {
                putExtra(EXTRA_TEMP_DELTA, tempDelta)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(intent)
            Log.i(TAG, "تم بث نية تعديل التكييف: delta=$tempDelta")
            true
        } catch (e: Exception) {
            Log.e(TAG, "فشل إرسال نية التكييف: ${e.message}")
            false
        }
    }

    open fun dispatchWindowCommand(action: String): Boolean {
        if (context == null) {
            Log.d(TAG, "[محاكاة] أمر النوافذ: $action")
            return true
        }

        return try {
            val intent = Intent(BYD_ACTION_WINDOW).apply {
                putExtra(EXTRA_WINDOW_STATE, action)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(intent)
            Log.i(TAG, "تم بث نية النوافذ: state=$action")
            true
        } catch (e: Exception) {
            Log.e(TAG, "فشل إرسال نية النوافذ: ${e.message}")
            false
        }
    }

    open fun dispatchSunroofCommand(action: String): Boolean {
        if (context == null) {
            Log.d(TAG, "[محاكاة] أمر فتحة السقف: $action")
            return true
        }

        return try {
            val intent = Intent(BYD_ACTION_SUNROOF).apply {
                putExtra(EXTRA_SUNROOF_STATE, action)
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            }
            context.sendBroadcast(intent)
            Log.i(TAG, "تم بث نية فتحة السقف: state=$action")
            true
        } catch (e: Exception) {
            Log.e(TAG, "فشل إرسال نية فتحة السقف: ${e.message}")
            false
        }
    }

    open fun dispatchVolumeCommand(adjustDirection: Int): Boolean {
        if (context == null) {
            Log.d(TAG, "[محاكاة] تعديل الصوت: direction=$adjustDirection")
            return true
        }

        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                adjustDirection,
                AudioManager.FLAG_SHOW_UI
            )
            Log.i(TAG, "تم تعديل مستوى الصوت: $adjustDirection")
            true
        } catch (e: Exception) {
            Log.e(TAG, "فشل تعديل مستوى الصوت: ${e.message}")
            false
        }
    }

    open fun dispatchMediaCommand(keyCode: Int): Boolean {
        if (context == null) {
            Log.d(TAG, "[محاكاة] إرسال مفتاح الوسائط: $keyCode")
            return true
        }

        return try {
            val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            }
            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
            context.sendOrderedBroadcast(downIntent, null)
            context.sendOrderedBroadcast(upIntent, null)
            Log.i(TAG, "تم إرسال حدث الوسائط: keyCode=$keyCode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "فشل إرسال حدث الوسائط: ${e.message}")
            false
        }
    }
}
