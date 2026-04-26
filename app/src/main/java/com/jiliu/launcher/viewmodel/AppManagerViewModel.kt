package com.jiliu.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jiliu.launcher.util.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val _allApps = MutableLiveData<List<AppUtils.AppInfo>>()
    val allApps: LiveData<List<AppUtils.AppInfo>> = _allApps

    private val _filteredApps = MutableLiveData<List<AppUtils.AppInfo>>()
    val filteredApps: LiveData<List<AppUtils.AppInfo>> = _filteredApps

    private val _selectedApp = MutableLiveData<AppUtils.AppInfo?>()
    val selectedApp: LiveData<AppUtils.AppInfo?> = _selectedApp

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _filterMode = MutableLiveData<FilterMode>()
    val filterMode: LiveData<FilterMode> = _filterMode

    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> = _searchQuery

    private var allAppsList: List<AppUtils.AppInfo> = emptyList()

    init {
        _filterMode.value = FilterMode.ALL
        _searchQuery.value = ""
        loadAllApps()
    }

    /**
     * Load all installed apps
     */
    fun loadAllApps(includeSystemApps: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                allAppsList = withContext(Dispatchers.IO) {
                    AppUtils.getInstalledApps(getApplication(), includeSystemApps)
                }
                _allApps.value = allAppsList
                applyFilters()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Set filter mode
     */
    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
        applyFilters()
    }

    /**
     * Set search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    /**
     * Apply filters
     */
    private fun applyFilters() {
        val mode = _filterMode.value ?: FilterMode.ALL
        val query = _searchQuery.value ?: ""
        
        var filtered = when (mode) {
            FilterMode.ALL -> allAppsList
            FilterMode.USER -> allAppsList.filter { !it.isSystemApp }
            FilterMode.SYSTEM -> allAppsList.filter { it.isSystemApp }
            FilterMode.APK -> allAppsList // Can add APK filter logic
        }
        
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        
        _filteredApps.value = filtered
    }

    /**
     * Select app
     */
    fun selectApp(app: AppUtils.AppInfo) {
        _selectedApp.value = app
    }

    /**
     * Clear selection
     */
    fun clearSelection() {
        _selectedApp.value = null
    }

    /**
     * Uninstall app
     */
    fun uninstallApp(packageName: String) {
        // This will be handled by the Activity
    }

    /**
     * Get app details
     */
    suspend fun getAppDetails(packageName: String): AppUtils.AppInfo? {
        return withContext(Dispatchers.IO) {
            allAppsList.find { it.packageName == packageName }
        }
    }

    enum class FilterMode {
        ALL,
        USER,
        SYSTEM,
        APK
    }
}
