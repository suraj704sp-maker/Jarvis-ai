package com.jarvis.ai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * JarvisAccessibilityService for Android 14 (API 34)
 * Allows JARVIS to:
 * 1. Navigate Back (performGlobalAction(GLOBAL_ACTION_BACK))
 * 2. Lock device screen (performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN))
 * 3. Swipe / Scroll screen up or down via GestureDescription
 * 4. Type text directly into the actively focused text field
 */
class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitors window changes or focus events if needed
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Executes the Android Global Back Action
     */
    fun performBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Locks the phone screen (Requires Android 9+ / Android 14)
     */
    fun lockScreen(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    /**
     * Dispatches a touch swipe gesture to scroll screen smoothly up or down
     */
    fun scrollScreen(isUp: Boolean) {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val centerX = (width / 2).toFloat()
        val startY: Float
        val endY: Float

        if (isUp) {
            // Scroll Up: Drag from top to bottom
            startY = height * 0.3f
            endY = height * 0.8f
        } else {
            // Scroll Down: Drag from bottom to top
            startY = height * 0.8f
            endY = height * 0.3f
        }

        val swipePath = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }

        val gestureBuilder = GestureDescription.Builder()
        val stroke = GestureDescription.StrokeDescription(swipePath, 0, 350)
        gestureBuilder.addStroke(stroke)

        dispatchGesture(gestureBuilder.build(), null, null)
    }

    /**
     * Types voice text directly into whichever text input is currently active on screen
     */
    fun typeTextIntoFocusedNode(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
}