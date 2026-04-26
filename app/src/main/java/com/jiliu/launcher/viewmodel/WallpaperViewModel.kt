package com.jiliu.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jiliu.launcher.model.WallpaperCategory
import com.jiliu.launcher.model.WallpaperInfo
import com.jiliu.launcher.repository.WallpaperRepository
import com.jiliu.launcher.util.WallpaperUtils
import kotlinx.coroutines.launch

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    private val wallpaperRepository = WallpaperRepository(application)

    private val _categories = MutableLiveData<List<WallpaperCategory>>()
    val categories: LiveData<List<WallpaperCategory>> = _categories

    private val _wallpapers = MutableLiveData<List<WallpaperInfo>>()
    val wallpapers: LiveData<List<WallpaperInfo>> = _wallpapers

    private val _featuredWallpapers = MutableLiveData<List<WallpaperInfo>>()
    val featuredWallpapers: LiveData<List<WallpaperInfo>> = _featuredWallpapers

    private val _selectedCategory = MutableLiveData<WallpaperCategory?>()
    val selectedCategory: LiveData<WallpaperCategory?> = _selectedCategory

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _setWallpaperResult = MutableLiveData<SetWallpaperResult?>()
    val setWallpaperResult: LiveData<SetWallpaperResult?> = _setWallpaperResult

    init {
        loadCategories()
        loadFeaturedWallpapers()
    }

    /**
     * Load categories
     */
    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _categories.value = wallpaperRepository.getCategories()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load wallpapers by category
     */
    fun loadWallpapersByCategory(categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val category = _categories.value?.find { it.id == categoryId }
                _selectedCategory.value = category
                _wallpapers.value = wallpaperRepository.getWallpapersByCategory(categoryId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load featured wallpapers
     */
    fun loadFeaturedWallpapers() {
        viewModelScope.launch {
            try {
                _featuredWallpapers.value = wallpaperRepository.getFeaturedWallpapers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Set wallpaper from URL
     */
    fun setWallpaperFromUrl(url: String, target: WallpaperUtils.WallpaperTarget = WallpaperUtils.WallpaperTarget.BOTH) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = wallpaperRepository.setWallpaperFromUrl(url, target)
                _setWallpaperResult.value = if (success) {
                    SetWallpaperResult.Success
                } else {
                    SetWallpaperResult.Error("设置壁纸失败")
                }
            } catch (e: Exception) {
                _setWallpaperResult.value = SetWallpaperResult.Error(e.message ?: "设置壁纸失败")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Set wallpaper from file
     */
    fun setWallpaperFromFile(filePath: String, target: WallpaperUtils.WallpaperTarget = WallpaperUtils.WallpaperTarget.BOTH) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = wallpaperRepository.setWallpaperFromFile(filePath, target)
                _setWallpaperResult.value = if (success) {
                    SetWallpaperResult.Success
                } else {
                    SetWallpaperResult.Error("设置壁纸失败")
                }
            } catch (e: Exception) {
                _setWallpaperResult.value = SetWallpaperResult.Error(e.message ?: "设置壁纸失败")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear wallpaper result
     */
    fun clearSetWallpaperResult() {
        _setWallpaperResult.value = null
    }

    sealed class SetWallpaperResult {
        object Success : SetWallpaperResult()
        data class Error(val message: String) : SetWallpaperResult()
    }
}
