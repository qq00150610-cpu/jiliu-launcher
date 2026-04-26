package com.jiliu.launcher.ui.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jiliu.launcher.App
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ActivitySettingsBinding
import com.jiliu.launcher.service.EdgeGestureService
import com.jiliu.launcher.service.FloatingDockService
import com.jiliu.launcher.ui.filemanager.FileManagerActivity
import com.jiliu.launcher.ui.wallpaper.WallpaperActivity
import com.jiliu.launcher.ui.member.MemberActivity
import com.jiliu.launcher.util.IntentUtils
import com.jiliu.launcher.util.PermissionUtils

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        updateUI()
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Home mode
        binding.cardHomeMode.setOnClickListener {
            showHomeModeDialog()
        }

        // Dock settings
        binding.cardDockSettings.setOnClickListener {
            showDockSettingsDialog()
        }

        // Floating dock
        binding.switchFloatingDock.setOnCheckedChangeListener { _, isChecked ->
            App.instance.preferencesManager.dockEnabled = isChecked
            if (isChecked) {
                startFloatingDock()
            } else {
                stopFloatingDock()
            }
        }

        // Music capsule
        binding.switchMusicCapsule.setOnCheckedChangeListener { _, isChecked ->
            App.instance.preferencesManager.musicCapsuleEnabled = isChecked
        }

        // Edge gesture
        binding.switchEdgeGesture.setOnCheckedChangeListener { _, isChecked ->
            App.instance.preferencesManager.edgeGestureEnabled = isChecked
            if (isChecked) {
                startEdgeGesture()
            } else {
                stopEdgeGesture()
            }
        }

        // Grid layout
        binding.cardGridLayout.setOnClickListener {
            showGridLayoutDialog()
        }

        // Wallpaper
        binding.cardWallpaper.setOnClickListener {
            startActivity(Intent(this, WallpaperActivity::class.java))
        }

        // Auto wallpaper
        binding.switchAutoWallpaper.setOnCheckedChangeListener { _, isChecked ->
            App.instance.preferencesManager.autoWallpaperEnabled = isChecked
        }

        // File manager
        binding.cardFileManager.setOnClickListener {
            startActivity(Intent(this, FileManagerActivity::class.java))
        }

        // App manager - navigate to AppManagerActivity
        binding.cardAppManager.setOnClickListener {
            Toast.makeText(this, "请在设置中打开", Toast.LENGTH_SHORT).show()
        }

        // Member/VIP
        binding.cardMember.setOnClickListener {
            startActivity(Intent(this, MemberActivity::class.java))
        }

        // System settings
        binding.cardSystemSettings.setOnClickListener {
            IntentUtils.openSettings(this)
        }

        // Permissions
        binding.cardPermissions.setOnClickListener {
            showPermissionsDialog()
        }

        // About
        binding.cardAbout.setOnClickListener {
            showAboutDialog()
        }

        // Cleanup
        binding.cardCleanup.setOnClickListener {
            startActivity(Intent(this, CleanupActivity::class.java))
        }

        // Recent tasks
        binding.cardRecentTasks.setOnClickListener {
            startActivity(Intent(this, RecentTasksActivity::class.java))
        }
    }

    private fun updateUI() {
        val prefs = App.instance.preferencesManager

        // Home mode
        binding.tvHomeModeValue.text = if (prefs.homeMode == 0) "简洁模式" else "丰富模式"

        // Dock
        binding.switchFloatingDock.isChecked = prefs.dockEnabled

        // Music capsule
        binding.switchMusicCapsule.isChecked = prefs.musicCapsuleEnabled

        // Edge gesture
        binding.switchEdgeGesture.isChecked = prefs.edgeGestureEnabled

        // Grid
        binding.tvGridValue.text = "${prefs.gridColumns}x${prefs.gridRows}"

        // Auto wallpaper
        binding.switchAutoWallpaper.isChecked = prefs.autoWallpaperEnabled

        // VIP status
        if (prefs.isVip) {
            binding.tvMemberStatus.text = "VIP会员"
            binding.tvMemberStatus.setTextColor(getColor(R.color.vip_gold))
        } else {
            binding.tvMemberStatus.text = "普通用户"
        }
    }

    private fun showHomeModeDialog() {
        val options = arrayOf("简洁模式", "丰富模式")
        val currentMode = App.instance.preferencesManager.homeMode

        android.app.AlertDialog.Builder(this)
            .setTitle("选择主页模式")
            .setSingleChoiceItems(options, currentMode) { dialog, which ->
                App.instance.preferencesManager.homeMode = which
                binding.tvHomeModeValue.text = options[which]
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDockSettingsDialog() {
        val options = arrayOf("启用Dock栏", "自定义Dock应用")
        
        android.app.AlertDialog.Builder(this)
            .setTitle("Dock栏设置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val isEnabled = binding.switchFloatingDock.isChecked
                        binding.switchFloatingDock.isChecked = !isEnabled
                    }
                    1 -> Toast.makeText(this, "功能开发中", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showGridLayoutDialog() {
        val options = arrayOf("3x4", "4x4", "5x4", "5x5", "6x4", "6x5")
        val currentColumns = App.instance.preferencesManager.gridColumns
        val currentRows = App.instance.preferencesManager.gridRows
        val currentIndex = options.indexOf("${currentColumns}x${currentRows}").coerceAtLeast(0)

        android.app.AlertDialog.Builder(this)
            .setTitle("选择网格布局")
            .setSingleChoiceItems(options, currentIndex) { dialog, which ->
                val parts = options[which].split("x")
                App.instance.preferencesManager.gridColumns = parts[0].toInt()
                App.instance.preferencesManager.gridRows = parts[1].toInt()
                binding.tvGridValue.text = options[which]
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPermissionsDialog() {
        val permissions = mutableListOf<String>()
        val status = PermissionUtils.checkAllRequiredPermissions(this)

        if (status.hasOverlayPermission) {
            permissions.add("✓ 悬浮窗权限")
        } else {
            permissions.add("✗ 悬浮窗权限")
        }

        if (status.hasStoragePermission) {
            permissions.add("✓ 存储权限")
        } else {
            permissions.add("✗ 存储权限")
        }

        if (status.hasWriteSettingsPermission) {
            permissions.add("✓ 系统设置权限")
        } else {
            permissions.add("✗ 系统设置权限")
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("权限状态")
            .setItems(permissions.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        if (!status.hasOverlayPermission) {
                            PermissionUtils.requestOverlayPermission(this)
                        }
                    }
                    1 -> {
                        if (!status.hasStoragePermission) {
                            PermissionUtils.requestStoragePermission(this)
                        }
                    }
                    2 -> {
                        if (!status.hasWriteSettingsPermission) {
                            PermissionUtils.requestWriteSettingsPermission(this)
                        }
                    }
                }
            }
            .setPositiveButton("打开权限设置") { _, _ ->
                IntentUtils.openAppSettings(this)
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showAboutDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("极流桌面")
            .setMessage("""
                版本: 1.0.0
                构建: 1
                
                极流桌面 - 国潮风格Android车机桌面
                为您的Android设备带来独特的国潮视觉体验
                
                © 2024 极流科技
            """.trimIndent())
            .setPositiveButton("确定", null)
            .show()
    }

    private fun startFloatingDock() {
        if (PermissionUtils.canDrawOverlays(this)) {
            startService(Intent(this, FloatingDockService::class.java))
        } else {
            PermissionUtils.requestOverlayPermission(this)
        }
    }

    private fun stopFloatingDock() {
        stopService(Intent(this, FloatingDockService::class.java))
    }

    private fun startEdgeGesture() {
        if (PermissionUtils.canDrawOverlays(this)) {
            startService(Intent(this, EdgeGestureService::class.java))
        } else {
            PermissionUtils.requestOverlayPermission(this)
        }
    }

    private fun stopEdgeGesture() {
        stopService(Intent(this, EdgeGestureService::class.java))
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
