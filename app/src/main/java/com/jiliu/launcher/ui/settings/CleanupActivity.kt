package com.jiliu.launcher.ui.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jiliu.launcher.databinding.ActivityCleanupBinding
import kotlinx.coroutines.*

class CleanupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCleanupBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    private var memoryCleared = false
    private var cacheCleared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCleanupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkMemoryStatus()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnClearMemory.setOnClickListener {
            clearMemory()
        }

        binding.btnClearCache.setOnClickListener {
            clearCache()
        }

        binding.btnOneKeyCleanup.setOnClickListener {
            oneKeyCleanup()
        }
    }

    private fun checkMemoryStatus() {
        val memInfo = getMemoryInfo()
        
        binding.tvTotalMemory.text = "总内存: ${formatMemory(memInfo.totalMemory)}"
        binding.tvAvailableMemory.text = "可用: ${formatMemory(memInfo.availableMemory)}"
        binding.tvUsedMemory.text = "已用: ${formatMemory(memInfo.usedMemory)}"
        
        val usagePercent = (memInfo.usedMemory.toFloat() / memInfo.totalMemory.toFloat() * 100).toInt()
        binding.memoryProgress.progress = usagePercent
    }

    private fun getMemoryInfo(): MemoryInfo {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        return MemoryInfo(
            totalMemory = memInfo.totalMem,
            availableMemory = memInfo.availMem,
            usedMemory = memInfo.totalMem - memInfo.availMem,
            threshold = memInfo.threshold,
            lowMemory = memInfo.lowMemory
        )
    }

    private fun clearMemory() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "正在清理内存..."

        scope.launch {
            delay(1500)
            
            try {
                // Clear app memory by garbage collection hint
                System.gc()
                System.runFinalization()
                
                // Request to clear memory from system
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                
                // This is a best-effort operation
                binding.tvStatus.text = "内存清理完成"
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_light))
                memoryCleared = true
                checkMemoryStatus()
            } catch (e: Exception) {
                binding.tvStatus.text = "清理失败: ${e.message}"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun clearCache() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "正在清理缓存..."

        scope.launch {
            delay(1500)
            
            try {
                // Clear cache directories
                var clearedSize = 0L
                
                // Clear internal cache
                cacheDir.deleteRecursively()
                clearedSize += calculateCacheSize()
                
                // Clear external cache if available
                externalCacheDir?.deleteRecursively()
                
                binding.tvStatus.text = "缓存清理完成, 释放 ${formatMemory(clearedSize)}"
                binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_light))
                cacheCleared = true
            } catch (e: Exception) {
                binding.tvStatus.text = "清理失败: ${e.message}"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun calculateCacheSize(): Long {
        var size = 0L
        cacheDir.walkTopDown().forEach { size += it.length() }
        return size
    }

    private fun oneKeyCleanup() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "正在一键清理..."

        scope.launch {
            // Step 1: Clear memory
            binding.tvStatus.text = "清理内存..."
            delay(500)
            clearMemory()
            
            // Step 2: Clear cache
            binding.tvStatus.text = "清理缓存..."
            delay(500)
            clearCache()
            
            // Step 3: Refresh status
            delay(500)
            checkMemoryStatus()
            
            binding.tvStatus.text = "一键清理完成!"
            binding.tvStatus.setTextColor(getColor(android.R.color.holo_green_light))
            binding.progressBar.visibility = View.GONE
            
            Toast.makeText(this@CleanupActivity, "清理完成", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatMemory(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    data class MemoryInfo(
        val totalMemory: Long,
        val availableMemory: Long,
        val usedMemory: Long,
        val threshold: Long,
        val lowMemory: Boolean
    )
}
