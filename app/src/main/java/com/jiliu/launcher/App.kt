package com.jiliu.launcher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.jiliu.launcher.util.PreferencesManager
import com.jiliu.launcher.util.ThemeManager

class App : Application(), ImageLoaderFactory {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var themeManager: ThemeManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize managers
        preferencesManager = PreferencesManager(this)
        themeManager = ThemeManager(this)

        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Music channel
            val musicChannel = NotificationChannel(
                CHANNEL_MUSIC,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音乐播放控制"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(musicChannel)

            // Floating service channel
            val floatingChannel = NotificationChannel(
                CHANNEL_FLOATING,
                "悬浮服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮窗口服务"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(floatingChannel)

            // System service channel
            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "系统服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "系统辅助服务"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(systemChannel)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    companion object {
        const val CHANNEL_MUSIC = "channel_music"
        const val CHANNEL_FLOATING = "channel_floating"
        const val CHANNEL_SYSTEM = "channel_system"

        lateinit var instance: App
            private set
    }
}
