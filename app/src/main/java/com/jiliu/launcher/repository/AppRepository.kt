package com.jiliu.launcher.repository

import android.content.Context
import android.content.pm.PackageManager
import com.jiliu.launcher.model.DesktopItem
import com.jiliu.launcher.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    /**
     * Get all installed apps
     */
    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<AppUtils.AppInfo> {
        return withContext(Dispatchers.IO) {
            AppUtils.getInstalledApps(context, includeSystemApps)
        }
    }

    /**
     * Get app info by package name
     */
    suspend fun getAppInfo(packageName: String): AppUtils.AppInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                AppUtils.AppInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = appInfo.loadIcon(pm),
                    isSystemApp = AppUtils.isSystemApp(appInfo),
                    versionName = AppUtils.getVersionName(context, packageName),
                    versionCode = AppUtils.getVersionCode(context, packageName),
                    installTime = AppUtils.getInstallTime(context, packageName),
                    updateTime = AppUtils.getUpdateTime(context, packageName)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Check if app is installed
     */
    fun isAppInstalled(packageName: String): Boolean {
        return AppUtils.isAppInstalled(context, packageName)
    }

    /**
     * Get app icon
     */
    fun getAppIcon(packageName: String) = AppUtils.getAppIcon(context, packageName)

    /**
     * Get app name
     */
    fun getAppName(packageName: String) = AppUtils.getAppName(context, packageName)

    /**
     * Convert AppInfo to DesktopItem
     */
    fun appInfoToDesktopItem(appInfo: AppUtils.AppInfo): DesktopItem {
        return DesktopItem(
            packageName = appInfo.packageName,
            appName = appInfo.appName,
            icon = appInfo.icon
        )
    }
}
