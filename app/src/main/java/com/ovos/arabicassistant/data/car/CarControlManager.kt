package com.ovos.arabicassistant.data.car

import android.media.AudioManager
import android.view.KeyEvent
import com.ovos.arabicassistant.domain.model.CarAction
import com.ovos.arabicassistant.domain.model.CarActionType

/**
 * مدير التحكم بعتاد السيارة
 * يربط بين نماذج الأفعال العتادية (CarAction) ومرسل النوايا الفعلي
 */
class CarControlManager(
    private val dispatcher: BydCarIntentDispatcher
) {

    fun execute(action: CarAction): Boolean {
        return when (action.type) {
            CarActionType.AC_TEMP -> {
                val delta = action.value.toIntOrNull() ?: -1
                dispatcher.dispatchAcCommand(delta)
            }
            CarActionType.AC_FAN -> {
                // تعديل سرعة المروحة
                dispatcher.dispatchAcCommand(0)
            }
            CarActionType.AC_TOGGLE -> {
                dispatcher.dispatchAcCommand(0)
            }
            CarActionType.WINDOW_OPEN -> {
                dispatcher.dispatchWindowCommand("open")
            }
            CarActionType.WINDOW_CLOSE -> {
                dispatcher.dispatchWindowCommand("close")
            }
            CarActionType.SUNROOF_OPEN -> {
                dispatcher.dispatchSunroofCommand("open")
            }
            CarActionType.SUNROOF_CLOSE -> {
                dispatcher.dispatchSunroofCommand("close")
            }
            CarActionType.VOLUME_UP -> {
                dispatcher.dispatchVolumeCommand(AudioManager.ADJUST_RAISE)
            }
            CarActionType.VOLUME_DOWN -> {
                dispatcher.dispatchVolumeCommand(AudioManager.ADJUST_LOWER)
            }
            CarActionType.VOLUME_MUTE -> {
                dispatcher.dispatchVolumeCommand(AudioManager.ADJUST_TOGGLE_MUTE)
            }
            CarActionType.MEDIA_PLAY_PAUSE -> {
                dispatcher.dispatchMediaCommand(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
            CarActionType.MEDIA_NEXT -> {
                dispatcher.dispatchMediaCommand(KeyEvent.KEYCODE_MEDIA_NEXT)
            }
            CarActionType.MEDIA_PREVIOUS -> {
                dispatcher.dispatchMediaCommand(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
        }
    }
}
