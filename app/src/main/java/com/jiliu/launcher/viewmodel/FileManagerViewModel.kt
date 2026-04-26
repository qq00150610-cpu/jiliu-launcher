package com.jiliu.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jiliu.launcher.util.AppUtils
import com.jiliu.launcher.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentPath = MutableLiveData<String>()
    val currentPath: LiveData<String> = _currentPath

    private val _files = MutableLiveData<List<FileUtils.FileItem>>()
    val files: LiveData<List<FileUtils.FileItem>> = _files

    private val _storages = MutableLiveData<List<FileUtils.StorageInfo>>()
    val storages: LiveData<List<FileUtils.StorageInfo>> = _storages

    private val _selectedFiles = MutableLiveData<Set<String>>()
    val selectedFiles: LiveData<Set<String>> = _selectedFiles

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _operationResult = MutableLiveData<OperationResult?>()
    val operationResult: LiveData<OperationResult?> = _operationResult

    private val _pathHistory = mutableListOf<String>()

    init {
        loadStorages()
        loadDefaultPath()
    }

    /**
     * Load available storages
     */
    fun loadStorages() {
        viewModelScope.launch {
            try {
                val allStorages = FileUtils.getAllStoragePaths(getApplication())
                _storages.value = allStorages
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Load default path
     */
    private fun loadDefaultPath() {
        val defaultPath = FileUtils.getExternalStoragePath()
        navigateToPath(defaultPath)
    }

    /**
     * Navigate to path
     */
    fun navigateToPath(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedFiles.value = emptySet()
            
            try {
                _currentPath.value = path
                _pathHistory.add(path)
                
                val fileList = withContext(Dispatchers.IO) {
                    FileUtils.getFilesInDirectory(path)
                }
                _files.value = fileList
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Navigate up
     */
    fun navigateUp(): Boolean {
        val currentPath = _currentPath.value ?: return false
        val parentPath = File(currentPath).parent
        
        if (parentPath != null) {
            navigateToPath(parentPath)
            return true
        }
        return false
    }

    /**
     * Navigate back in history
     */
    fun navigateBack(): Boolean {
        if (_pathHistory.size > 1) {
            _pathHistory.removeAt(_pathHistory.size - 1)
            val previousPath = _pathHistory.lastOrNull() ?: return false
            navigateToPath(previousPath)
            _pathHistory.removeAt(_pathHistory.size - 1)
            return true
        }
        return false
    }

    /**
     * Select file
     */
    fun selectFile(path: String) {
        val current = _selectedFiles.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _selectedFiles.value = current
    }

    /**
     * Select all files
     */
    fun selectAllFiles() {
        val allPaths = _files.value?.map { it.path }?.toSet() ?: emptySet()
        _selectedFiles.value = allPaths
    }

    /**
     * Clear selection
     */
    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }

    /**
     * Toggle selection
     */
    fun toggleSelection(path: String) {
        selectFile(path)
    }

    /**
     * Delete selected files
     */
    fun deleteSelectedFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            val selected = _selectedFiles.value ?: emptySet()
            var successCount = 0
            var failCount = 0
            
            withContext(Dispatchers.IO) {
                for (path in selected) {
                    if (FileUtils.deleteFile(path)) {
                        successCount++
                    } else {
                        failCount++
                    }
                }
            }
            
            _selectedFiles.value = emptySet()
            _operationResult.value = OperationResult.Delete(successCount, failCount)
            refreshCurrentDirectory()
            _isLoading.value = false
        }
    }

    /**
     * Create folder
     */
    fun createFolder(name: String) {
        viewModelScope.launch {
            val currentPath = _currentPath.value ?: return@launch
            
            withContext(Dispatchers.IO) {
                FileUtils.createDirectory("$currentPath/$name")
            }
            
            refreshCurrentDirectory()
        }
    }

    /**
     * Rename file
     */
    fun renameFile(oldPath: String, newName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                FileUtils.renameFile(oldPath, newName)
            }
            refreshCurrentDirectory()
        }
    }

    /**
     * Copy files
     */
    fun copyFiles(sourcePaths: List<String>, destPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0
            var failCount = 0
            
            withContext(Dispatchers.IO) {
                for (sourcePath in sourcePaths) {
                    val fileName = File(sourcePath).name
                    val destFilePath = "$destPath/$fileName"
                    if (FileUtils.copyFile(sourcePath, destFilePath)) {
                        successCount++
                    } else {
                        failCount++
                    }
                }
            }
            
            _operationResult.value = OperationResult.Copy(successCount, failCount)
            _isLoading.value = false
        }
    }

    /**
     * Move files
     */
    fun moveFiles(sourcePaths: List<String>, destPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0
            var failCount = 0
            
            withContext(Dispatchers.IO) {
                for (sourcePath in sourcePaths) {
                    val fileName = File(sourcePath).name
                    val destFilePath = "$destPath/$fileName"
                    if (FileUtils.moveFile(sourcePath, destFilePath)) {
                        successCount++
                    } else {
                        failCount++
                    }
                }
            }
            
            _operationResult.value = OperationResult.Move(successCount, failCount)
            refreshCurrentDirectory()
            _isLoading.value = false
        }
    }

    /**
     * Refresh current directory
     */
    fun refreshCurrentDirectory() {
        _currentPath.value?.let { navigateToPath(it) }
    }

    /**
     * Clear operation result
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }

    sealed class OperationResult {
        data class Delete(val successCount: Int, val failCount: Int) : OperationResult()
        data class Copy(val successCount: Int, val failCount: Int) : OperationResult()
        data class Move(val successCount: Int, val failCount: Int) : OperationResult()
    }
}
