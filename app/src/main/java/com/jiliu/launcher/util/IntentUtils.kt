package com.jiliu.launcher.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.io.File

object IntentUtils {

    /**
     * Open app by package name
     */
    fun openApp(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Open app details settings
     */
    fun openAppDetails(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Open system settings
     */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Open specific settings page
     */
    fun openSettingsPage(context: Context, action: String) {
        try {
            val intent = Intent(action)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            openSettings(context)
        }
    }

    /**
     * Open display settings
     */
    fun openDisplaySettings(context: Context) {
        openSettingsPage(context, Settings.ACTION_DISPLAY_SETTINGS)
    }

    /**
     * Open sound settings
     */
    fun openSoundSettings(context: Context) {
        openSettingsPage(context, Settings.ACTION_SOUND_SETTINGS)
    }

    /**
     * Open network settings
     */
    fun openNetworkSettings(context: Context) {
        openSettingsPage(context, Settings.ACTION_WIRELESS_SETTINGS)
    }

    /**
     * Open Bluetooth settings
     */
    fun openBluetoothSettings(context: Context) {
        openSettingsPage(context, Settings.ACTION_BLUETOOTH_SETTINGS)
    }

    /**
     * Open app info
     */
    fun openAppInfoSettings(context: Context) {
        openAppDetails(context, context.packageName)
    }

    /**
     * Open app settings
     */
    fun openAppSettings(context: Context) {
        openAppDetails(context, context.packageName)
    }

    /**
     * Open file with specific app
     */
    fun openFile(context: Context, filePath: String, mimeType: String? = null) {
        try {
            val file = File(filePath)
            val uri = FileProviderUtil.getUriForFile(context, file)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType ?: getMimeType(filePath))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (isIntentAvailable(context, intent)) {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Open video player
     */
    fun openVideoPlayer(context: Context, filePath: String) {
        openFile(context, filePath, "video/*")
    }

    /**
     * Open music player
     */
    fun openMusicPlayer(context: Context, filePath: String) {
        openFile(context, filePath, "audio/*")
    }

    /**
     * Open image viewer
     */
    fun openImageViewer(context: Context, filePath: String) {
        openFile(context, filePath, "image/*")
    }

    /**
     * Share file
     */
    fun shareFile(context: Context, filePath: String, mimeType: String? = null) {
        try {
            val file = File(filePath)
            val uri = FileProviderUtil.getUriForFile(context, file)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType ?: getMimeType(filePath)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "分享").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Uninstall app
     */
    fun uninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Install APK
     */
    fun installApk(context: Context, apkPath: String) {
        try {
            val file = File(apkPath)
            val uri = FileProviderUtil.getUriForFile(context, file)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check if intent is available
     */
    fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return try {
            val list = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            list.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get MIME type from file extension
     */
    private fun getMimeType(filePath: String): String {
        val extension = FileUtils.getFileExtension(filePath)
        return FileUtils.getMimeType(extension)
    }

    /**
     * Open accessibility settings
     */
    fun openAccessibilitySettings(context: Context) {
        openSettingsPage(context, Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    /**
     * Open notification settings
     */
    fun openNotificationSettings(context: Context) {
        openSettingsPage(context, Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    }

    /**
     * Open battery settings
     */
    fun openBatterySettings(context: Context) {
        openSettingsPage(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)
    }

    /**
     * Open storage settings
     */
    fun openStorageSettings(context: Context) {
        openSettingsPage(context, Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
    }

    /**
     * Open date/time settings
     */
    fun openDateTimeSettings(context: Context) {
        openSettingsPage(context, Settings.ACTION_DATE_SETTINGS)
    }

    /**
     * Open language settings
     */
    fun openLanguageSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            openSettingsPage(context, Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
        } else {
            openSettingsPage(context, Settings.ACTION_LOCALE_SETTINGS)
        }
    }

    /**
     * Check if app is installed
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
