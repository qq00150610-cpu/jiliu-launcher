package com.jiliu.launcher.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import java.io.File

object AppUtils {

    /**
     * Get app icon by package name
     */
    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get app name by package name
     */
    fun getAppName(context: Context, packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * Check if app is installed
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get all installed apps
     */
    fun getInstalledApps(context: Context, includeSystemApps: Boolean = false): List<AppInfo> {
        val pm = context.packageManager
        val apps = mutableListOf<AppInfo>()

        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            // Skip system apps if not requested
            if (!includeSystemApps && isSystemApp(appInfo)) {
                continue
            }

            apps.add(
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = appInfo.loadIcon(pm),
                    isSystemApp = isSystemApp(appInfo),
                    versionName = getVersionName(context, appInfo.packageName),
                    versionCode = getVersionCode(context, appInfo.packageName),
                    installTime = getInstallTime(context, appInfo.packageName),
                    updateTime = getUpdateTime(context, appInfo.packageName)
                )
            )
        }

        return apps.sortedBy { it.appName.lowercase() }
    }

    /**
     * Check if app is a system app
     */
    fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    /**
     * Get app version name
     */
    fun getVersionName(context: Context, packageName: String): String? {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get app version code
     */
    fun getVersionCode(context: Context, packageName: String): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get app install time
     */
    fun getInstallTime(context: Context, packageName: String): Long {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).firstInstallTime
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get app update time
     */
    fun getUpdateTime(context: Context, packageName: String): Long {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get APK file path
     */
    fun getApkPath(context: Context, packageName: String): String? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            appInfo.sourceDir
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get app size
     */
    fun getAppSize(context: Context, packageName: String): Long {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            File(appInfo.sourceDir).length()
        } catch (e: Exception) {
            0L
        }
    }

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val icon: Drawable,
        val isSystemApp: Boolean = false,
        val versionName: String? = null,
        val versionCode: Long = 0,
        val installTime: Long = 0,
        val updateTime: Long = 0
    )
}
