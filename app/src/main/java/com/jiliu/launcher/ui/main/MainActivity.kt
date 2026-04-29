package com.jiliu.launcher.ui.main

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.jiliu.launcher.App
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ActivityMainBinding
import com.jiliu.launcher.service.FloatingDockService
import com.jiliu.launcher.ui.dock.DockFragment
import com.jiliu.launcher.ui.home1.Home1Fragment
import com.jiliu.launcher.ui.home2.Home2Fragment
import com.jiliu.launcher.util.PermissionUtils
import com.jiliu.launcher.util.WallpaperUtils
import com.jiliu.launcher.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private var currentHomeFragment: Fragment? = null

    // 壁纸选择器
    private val wallpaperPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            // 用户选择了壁纸
            loadCustomWallpaper(selectedUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup immersive mode
        setupImmersiveMode()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set wallpaper background
        setWallpaperBackground()
        
        // Check permissions
        checkPermissions()
        
        // Initialize home fragment
        if (savedInstanceState == null) {
            initializeHomeFragment()
        }
        
        // Setup bottom navigation or dock
        setupDock()
        
        // Observe home mode changes
        observeHomeMode()
    }

    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Hide system bars for fullscreen
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        // Keep screen on during car mode
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setWallpaperBackground() {
        // 优先使用自定义壁纸
        val customWallpaperPath = App.instance.preferencesManager.lastWallpaperPath
        
        if (!customWallpaperPath.isNullOrEmpty()) {
            val wallpaperFile = File(customWallpaperPath)
            if (wallpaperFile.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(wallpaperFile.absolutePath)
                    if (bitmap != null) {
                        binding.wallpaperImage.setImageBitmap(bitmap)
                        binding.wallpaperImage.visibility = View.VISIBLE
                        return
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // 回退到系统壁纸
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            val wallpaperDrawable = wallpaperManager.drawable
            
            if (wallpaperDrawable != null) {
                binding.root.background = wallpaperDrawable
            } else {
                // 默认深色背景
                binding.root.setBackgroundResource(R.color.background_dark)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.root.setBackgroundResource(R.color.background_dark)
        }
    }

    /**
     * 加载自定义壁纸
     */
    fun loadCustomWallpaper(uri: android.net.Uri) {
        try {
            // 保存壁纸路径
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                // 保存到应用私有目录
                val wallpaperDir = File(filesDir, "wallpaper")
                if (!wallpaperDir.exists()) {
                    wallpaperDir.mkdirs()
                }
                
                val wallpaperFile = File(wallpaperDir, "custom_wallpaper.jpg")
                wallpaperFile.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                
                // 保存路径
                App.instance.preferencesManager.lastWallpaperPath = wallpaperFile.absolutePath
                
                // 显示壁纸
                binding.wallpaperImage.setImageBitmap(bitmap)
                binding.wallpaperImage.visibility = View.VISIBLE
                binding.root.background = null
                
                Toast.makeText(this, "壁纸设置成功", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "壁纸设置失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开壁纸选择器
     */
    fun openWallpaperPicker() {
        wallpaperPicker.launch("image/*")
    }

    /**
     * 清除自定义壁纸，使用系统壁纸
     */
    fun clearCustomWallpaper() {
        App.instance.preferencesManager.lastWallpaperPath = null
        setWallpaperBackground()
    }

    private fun checkPermissions() {
        val status = PermissionUtils.checkAllRequiredPermissions(this)
        
        if (!status.isAllGranted) {
            // Show permission request or handle gracefully
            // For launcher, we should work even without all permissions
        }
    }

    private fun initializeHomeFragment() {
        val homeMode = App.instance.preferencesManager.homeMode
        showHomeFragment(homeMode)
    }

    private fun showHomeFragment(mode: Int) {
        val fragment = when (mode) {
            0 -> Home1Fragment()
            1 -> Home2Fragment()
            else -> Home1Fragment()
        }
        
        supportFragmentManager.commit {
            replace(R.id.home_container, fragment)
        }
        
        currentHomeFragment = fragment
    }

    private fun observeHomeMode() {
        viewModel.homeMode.observe(this) { mode ->
            val currentMode = App.instance.preferencesManager.homeMode
            if (mode != currentMode) {
                showHomeFragment(mode)
            }
        }
    }

    private fun setupDock() {
        if (App.instance.preferencesManager.dockEnabled) {
            supportFragmentManager.commit {
                replace(R.id.dock_container, DockFragment())
            }
            binding.dockContainer.visibility = View.VISIBLE
        } else {
            binding.dockContainer.visibility = View.GONE
        }
    }

    /**
     * Toggle home mode between home1 and home2
     */
    fun toggleHomeMode() {
        val currentMode = App.instance.preferencesManager.homeMode
        val newMode = if (currentMode == 0) 1 else 0
        App.instance.preferencesManager.homeMode = newMode
        viewModel.setHomeMode(newMode)
        showHomeFragment(newMode)
    }

    /**
     * Open settings
     */
    fun openSettings() {
        startActivity(Intent(this, com.jiliu.launcher.ui.settings.SettingsActivity::class.java))
    }

    /**
     * Open file manager
     */
    fun openFileManager() {
        startActivity(Intent(this, com.jiliu.launcher.ui.filemanager.FileManagerActivity::class.java))
    }

    /**
     * Open app manager
     */
    fun openAppManager() {
        startActivity(Intent(this, com.jiliu.launcher.ui.appmanager.AppManagerActivity::class.java))
    }

    /**
     * Open wallpaper
     */
    fun openWallpaper() {
        startActivity(Intent(this, com.jiliu.launcher.ui.wallpaper.WallpaperActivity::class.java))
    }

    /**
     * Open member/VIP
     */
    fun openMember() {
        startActivity(Intent(this, com.jiliu.launcher.ui.member.MemberActivity::class.java))
    }

    /**
     * Toggle floating dock
     */
    fun toggleFloatingDock() {
        val intent = Intent(this, FloatingDockService::class.java)
        if (PermissionUtils.canDrawOverlays(this)) {
            if (FloatingDockService.isRunning) {
                stopService(intent)
            } else {
                startService(intent)
            }
        } else {
            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show()
        }
    }
}
