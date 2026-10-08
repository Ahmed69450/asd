package com.ovos.arabicassistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * خدمة إمكانية الوصول لشاشة سيارة BYD DiLink
 * تراقب تغيرات الشاشة وتستخرج شجرة عناصر واجهة المستخدم (UI Nodes)
 * وتغذي ScreenContextHolder لسياق المحادثة عبر بروتوكول MCP.
 */
class CarAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "CarAccessibility"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            val packageName = event.packageName?.toString() ?: ""
            // تجنب قراءة نصوص تطبيق المساعد نفسه لمنع التغذية العكسية اللانهائية
            if (packageName == this.packageName) return

            val rootNode = rootInActiveWindow ?: return
            try {
                val collectedTexts = mutableListOf<String>()
                extractTextFromNode(rootNode, collectedTexts)
                ScreenContextHolder.update(packageName, collectedTexts)
            } catch (e: Exception) {
                Log.w(TAG, "خطأ أثناء قراءة عقد واجهة الشاشة: ${e.message}")
            } finally {
                @Suppress("DEPRECATION")
                rootNode.recycle()
            }
        }
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo?, output: MutableList<String>) {
        if (node == null || output.size >= 25) return

        val text = node.text?.toString()?.trim()
        val contentDesc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrBlank() && text.length > 1) {
            output.add(text)
        } else if (!contentDesc.isNullOrBlank() && contentDesc.length > 1) {
            output.add(contentDesc)
        }

        for (i in 0 until node.childCount) {
            if (output.size >= 25) break
            val child = node.getChild(i)
            if (child != null) {
                extractTextFromNode(child, output)
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "تمت مقاطعة خدمة إمكانية الوصول للشاشة.")
    }

    override fun onDestroy() {
        super.onDestroy()
        ScreenContextHolder.clear()
        Log.i(TAG, "تم إغلاق خدمة إمكانية الوصول للشاشة.")
    }
}
