package com.jiliu.launcher.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.jiliu.launcher.App
import com.jiliu.launcher.R
import com.jiliu.launcher.ui.main.MainActivity
import kotlinx.coroutines.*

class FloatingDockService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: android.view.View? = null
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, createNotification())
        createFloatingWidget()
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

        val stopIntent = Intent(this, FloatingDockService::class.java)
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, App.CHANNEL_FLOATING)
            .setContentTitle("极流桌面")
            .setContentText("悬浮Dock栏服务运行中")
            .setSmallIcon(R.drawable.ic_dock)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_close, "停止", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createFloatingWidget() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater
        
        // In a real implementation, you would inflate a custom layout
        // For now, we'll create a simple floating view
        floatingView = android.view.View(this).apply {
            setBackgroundColor(0x88000000.toInt())
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 100
        }

        try {
            windowManager?.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatingWidget() {
        try {
            floatingView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingWidget()
        serviceJob.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        var isRunning = false
            private set

        private const val NOTIFICATION_ID = 1001
    }

    init {
        isRunning = true
    }
}
