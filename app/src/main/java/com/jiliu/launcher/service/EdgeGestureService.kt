package com.jiliu.launcher.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.jiliu.launcher.App
import com.jiliu.launcher.R
import com.jiliu.launcher.ui.main.MainActivity

class EdgeGestureService : Service() {

    private var windowManager: WindowManager? = null
    private var gestureView: android.view.View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, createNotification())
        createGestureOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, EdgeGestureService::class.java)
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, App.CHANNEL_FLOATING)
            .setContentTitle("极流桌面")
            .setContentText("边缘手势服务运行中")
            .setSmallIcon(R.drawable.ic_gesture)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_close, "停止", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createGestureOverlay() {
        gestureView = android.view.View(this).apply {
            // Create a transparent overlay to detect edge gestures
            setBackgroundColor(0x00000000)
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager?.addView(gestureView, layoutParams)
            setupGestureDetection()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupGestureDetection() {
        // Setup touch listener for edge gesture detection
        // This is a simplified implementation
        // In production, you would use proper gesture detection
    }

    private fun removeGestureOverlay() {
        try {
            gestureView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeGestureOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        private const val NOTIFICATION_ID = 1003
    }
}
