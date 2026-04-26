package com.jiliu.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jiliu.launcher.App
import com.jiliu.launcher.model.DesktopItem
import com.jiliu.launcher.repository.AppRepository
import com.jiliu.launcher.util.AppUtils
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)

    private val _installedApps = MutableLiveData<List<AppUtils.AppInfo>>()
    val installedApps: LiveData<List<AppUtils.AppInfo>> = _installedApps

    private val _desktopItems = MutableLiveData<List<DesktopItem>>()
    val desktopItems: LiveData<List<DesktopItem>> = _desktopItems

    private val _homeMode = MutableLiveData<Int>()
    val homeMode: LiveData<Int> = _homeMode

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadHomeMode()
        loadInstalledApps()
    }

    /**
     * Load installed apps
     */
    fun loadInstalledApps(includeSystemApps: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apps = appRepository.getInstalledApps(includeSystemApps)
                _installedApps.value = apps
                
                // Convert to desktop items
                val items = apps.map { appRepository.appInfoToDesktopItem(it) }
                _desktopItems.value = items
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load home mode
     */
    private fun loadHomeMode() {
        _homeMode.value = App.instance.preferencesManager.homeMode
    }

    /**
     * Set home mode
     */
    fun setHomeMode(mode: Int) {
        App.instance.preferencesManager.homeMode = mode
        _homeMode.value = mode
    }

    /**
     * Refresh apps
     */
    fun refreshApps() {
        loadInstalledApps()
    }

    /**
     * Check if app is installed
     */
    fun isAppInstalled(packageName: String): Boolean {
        return appRepository.isAppInstalled(packageName)
    }

    /**
     * Get app icon
     */
    fun getAppIcon(packageName: String) = appRepository.getAppIcon(packageName)

    /**
     * Get app name
     */
    fun getAppName(packageName: String) = appRepository.getAppName(packageName)
}
