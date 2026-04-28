package com.jiliu.launcher.util

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jiliu_prefs")

class PreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Home mode: 0 = simple (home1), 1 = rich (home2)
    var homeMode: Int
        get() = prefs.getInt(KEY_HOME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_HOME_MODE, value).apply()

    // Dock visibility
    var dockEnabled: Boolean
        get() = prefs.getBoolean(KEY_DOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DOCK_ENABLED, value).apply()

    // Music capsule visibility
    var musicCapsuleEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_CAPSULE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MUSIC_CAPSULE_ENABLED, value).apply()

    // Edge gesture enabled
    var edgeGestureEnabled: Boolean
        get() = prefs.getBoolean(KEY_EDGE_GESTURE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_EDGE_GESTURE_ENABLED, value).apply()

    // VIP status
    var isVip: Boolean
        get() = prefs.getBoolean(KEY_IS_VIP, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_VIP, value).apply()

    // VIP expiration time
    var vipExpireTime: Long
        get() = prefs.getLong(KEY_VIP_EXPIRE_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_VIP_EXPIRE_TIME, value).apply()

    // User data (JSON)
    var currentUserJson: String?
        get() = prefs.getString(KEY_CURRENT_USER, null)
        set(value) = prefs.edit().putString(KEY_CURRENT_USER, value).apply()

    // User password hash
    var userPasswordHash: String?
        get() = prefs.getString(KEY_USER_PASSWORD_HASH, null)
        set(value) = prefs.edit().putString(KEY_USER_PASSWORD_HASH, value).apply()

    // Last verification code (for mock)
    var lastVerificationCode: String?
        get() = prefs.getString(KEY_VERIFICATION_CODE, null)
        set(value) = prefs.edit().putString(KEY_VERIFICATION_CODE, value).apply()

    // Grid columns
    var gridColumns: Int
        get() = prefs.getInt(KEY_GRID_COLUMNS, 5)
        set(value) = prefs.edit().putInt(KEY_GRID_COLUMNS, value).apply()

    // Grid rows
    var gridRows: Int
        get() = prefs.getInt(KEY_GRID_ROWS, 4)
        set(value) = prefs.edit().putInt(KEY_GRID_ROWS, value).apply()

    // Auto wallpaper change enabled
    var autoWallpaperEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_WALLPAPER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_WALLPAPER_ENABLED, value).apply()

    // Auto wallpaper interval (hours)
    var autoWallpaperInterval: Int
        get() = prefs.getInt(KEY_AUTO_WALLPAPER_INTERVAL, 24)
        set(value) = prefs.edit().putInt(KEY_AUTO_WALLPAPER_INTERVAL, value).apply()

    // Last wallpaper path
    var lastWallpaperPath: String?
        get() = prefs.getString(KEY_LAST_WALLPAPER_PATH, null)
        set(value) = prefs.edit().putString(KEY_LAST_WALLPAPER_PATH, value).apply()

    // Dock apps (comma separated package names)
    var dockApps: String
        get() = prefs.getString(KEY_DOCK_APPS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DOCK_APPS, value).apply()

    // Accessibility service enabled
    var accessibilityEnabled: Boolean
        get() = prefs.getBoolean(KEY_ACCESSIBILITY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ACCESSIBILITY_ENABLED, value).apply()

    // First launch
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    // Bluetooth auto play enabled
    var bluetoothAutoPlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLUETOOTH_AUTO_PLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_BLUETOOTH_AUTO_PLAY, value).apply()

    // Generic get/set methods
    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    // DataStore-based flow for reactive updates
    val homeModeFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.HOME_MODE] ?: 0
    }

    suspend fun setHomeMode(mode: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HOME_MODE] = mode
        }
    }

    suspend fun getHomeMode(): Int {
        return context.dataStore.data.first()[PreferencesKeys.HOME_MODE] ?: 0
    }

    private object PreferencesKeys {
        val HOME_MODE = intPreferencesKey("home_mode")
    }

    companion object {
        private const val PREFS_NAME = "jiliu_launcher_prefs"
        
        private const val KEY_HOME_MODE = "home_mode"
        private const val KEY_DOCK_ENABLED = "dock_enabled"
        private const val KEY_MUSIC_CAPSULE_ENABLED = "music_capsule_enabled"
        private const val KEY_EDGE_GESTURE_ENABLED = "edge_gesture_enabled"
        private const val KEY_IS_VIP = "is_vip"
        private const val KEY_VIP_EXPIRE_TIME = "vip_expire_time"
        private const val KEY_CURRENT_USER = "current_user"
        private const val KEY_USER_PASSWORD_HASH = "user_password_hash"
        private const val KEY_VERIFICATION_CODE = "verification_code"
        private const val KEY_GRID_COLUMNS = "grid_columns"
        private const val KEY_GRID_ROWS = "grid_rows"
        private const val KEY_AUTO_WALLPAPER_ENABLED = "auto_wallpaper_enabled"
        private const val KEY_AUTO_WALLPAPER_INTERVAL = "auto_wallpaper_interval"
        private const val KEY_LAST_WALLPAPER_PATH = "last_wallpaper_path"
        private const val KEY_DOCK_APPS = "dock_apps"
        private const val KEY_ACCESSIBILITY_ENABLED = "accessibility_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_BLUETOOTH_AUTO_PLAY = "bluetooth_auto_play"
    }
}
