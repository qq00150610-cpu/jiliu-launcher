package com.jiliu.launcher.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.jiliu.launcher.App

class GlobalBackAccessibilityService : AccessibilityService() {

    private var lastBackPressTime = 0L
    private val doublePressThreshold = 500L

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        
        // Save service state
        App.instance.preferencesManager.accessibilityEnabled = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events if needed
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onKeyEvent(event: android.view.KeyEvent?): Boolean {
        if (event?.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            val currentTime = System.currentTimeMillis()
            
            if (event.action == android.view.KeyEvent.ACTION_UP) {
                // Check for double press to exit
                if (currentTime - lastBackPressTime < doublePressThreshold) {
                    // Double back press detected
                    return false // Let system handle it
                }
                lastBackPressTime = currentTime
            }
            
            // Single back press - can add custom handling here
            return false
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        App.instance.preferencesManager.accessibilityEnabled = false
    }
}
