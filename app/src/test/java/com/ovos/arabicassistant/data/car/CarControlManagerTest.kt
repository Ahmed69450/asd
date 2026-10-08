package com.ovos.arabicassistant.data.car

import com.ovos.arabicassistant.domain.model.CarAction
import com.ovos.arabicassistant.domain.model.CarActionType
import org.junit.Assert.assertTrue
import org.junit.Test

class CarControlManagerTest {

    private class FakeIntentDispatcher : BydCarIntentDispatcher(null) {
        var lastAcDelta: Int? = null
        var lastWindowCmd: String? = null
        var lastSunroofCmd: String? = null
        var lastMediaKey: Int? = null
        var lastVolumeAdjust: Int? = null

        override fun dispatchAcCommand(tempDelta: Int): Boolean {
            lastAcDelta = tempDelta
            return true
        }

        override fun dispatchWindowCommand(action: String): Boolean {
            lastWindowCmd = action
            return true
        }

        override fun dispatchSunroofCommand(action: String): Boolean {
            lastSunroofCmd = action
            return true
        }

        override fun dispatchMediaCommand(keyCode: Int): Boolean {
            lastMediaKey = keyCode
            return true
        }

        override fun dispatchVolumeCommand(adjustDirection: Int): Boolean {
            lastVolumeAdjust = adjustDirection
            return true
        }
    }

    @Test
    fun `dispatches ac temperature adjustment`() {
        val fakeDispatcher = FakeIntentDispatcher()
        val manager = CarControlManager(fakeDispatcher)

        val result = manager.execute(CarAction(CarActionType.AC_TEMP, "-1"))
        assertTrue(result)
        org.junit.Assert.assertEquals(-1, fakeDispatcher.lastAcDelta)
    }

    @Test
    fun `dispatches window and sunroof actions`() {
        val fakeDispatcher = FakeIntentDispatcher()
        val manager = CarControlManager(fakeDispatcher)

        assertTrue(manager.execute(CarAction(CarActionType.WINDOW_OPEN, "all")))
        org.junit.Assert.assertEquals("open", fakeDispatcher.lastWindowCmd)

        assertTrue(manager.execute(CarAction(CarActionType.SUNROOF_CLOSE, "")))
        org.junit.Assert.assertEquals("close", fakeDispatcher.lastSunroofCmd)
    }

    @Test
    fun `dispatches volume and playback controls`() {
        val fakeDispatcher = FakeIntentDispatcher()
        val manager = CarControlManager(fakeDispatcher)

        assertTrue(manager.execute(CarAction(CarActionType.VOLUME_UP, "+2")))
        org.junit.Assert.assertEquals(1, fakeDispatcher.lastVolumeAdjust)

        assertTrue(manager.execute(CarAction(CarActionType.MEDIA_NEXT, "")))
        org.junit.Assert.assertEquals(android.view.KeyEvent.KEYCODE_MEDIA_NEXT, fakeDispatcher.lastMediaKey)
    }
}
