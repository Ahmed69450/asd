package com.ovos.arabicassistant.domain.model

/**
 * أنواع أوامر عتاد السيارة المدعومة في نظام BYD DiLink
 */
enum class CarActionType {
    AC_TEMP,
    AC_FAN,
    AC_TOGGLE,
    WINDOW_OPEN,
    WINDOW_CLOSE,
    SUNROOF_OPEN,
    SUNROOF_CLOSE,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    MEDIA_PLAY_PAUSE,
    MEDIA_NEXT,
    MEDIA_PREVIOUS
}

/**
 * تمثيل أمر مادي للسيارة
 */
data class CarAction(
    val type: CarActionType,
    val value: String = "",
    val extraParams: Map<String, String> = emptyMap()
)
