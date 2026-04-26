package com.jiliu.launcher.ui.appmanager

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiliu.launcher.databinding.ActivityAppManagerBinding
import com.jiliu.launcher.util.AppUtils
import com.jiliu.launcher.util.IntentUtils
import com.jiliu.launcher.viewmodel.AppManagerViewModel

class AppManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppManagerBinding
    private val viewModel: AppManagerViewModel by viewModels()

    private lateinit var appAdapter: AppManagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Search
        binding.searchView.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        // Filter tabs
        binding.tabAll.setOnClickListener { viewModel.setFilterMode(AppManagerViewModel.FilterMode.ALL) }
        binding.tabUser.setOnClickListener { viewModel.setFilterMode(AppManagerViewModel.FilterMode.USER) }
        binding.tabSystem.setOnClickListener { viewModel.setFilterMode(AppManagerViewModel.FilterMode.SYSTEM) }

        // App list
        appAdapter = AppManagerAdapter(
            onAppClick = { app ->
                showAppDetails(app)
            },
            onAppLongClick = { app ->
                showAppOptions(app)
            }
        )

        binding.appRecycler.apply {
            layoutManager = LinearLayoutManager(this@AppManagerActivity)
            adapter = appAdapter
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            viewModel.loadAllApps()
        }

        // Observe filter mode
        viewModel.filterMode.observe(this) { mode ->
            updateFilterTabs(mode)
        }
    }

    private fun observeData() {
        viewModel.filteredApps.observe(this) { apps ->
            appAdapter.submitList(apps)
            binding.tvEmpty.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun updateFilterTabs(mode: AppManagerViewModel.FilterMode) {
        binding.tabAll.isSelected = mode == AppManagerViewModel.FilterMode.ALL
        binding.tabUser.isSelected = mode == AppManagerViewModel.FilterMode.USER
        binding.tabSystem.isSelected = mode == AppManagerViewModel.FilterMode.SYSTEM
    }

    private fun showAppDetails(app: AppUtils.AppInfo) {
        viewModel.selectApp(app)
        
        AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setMessage("""
                应用名称: ${app.appName}
                包名: ${app.packageName}
                版本: ${app.versionName ?: "未知"}
                版本号: ${app.versionCode}
                安装时间: ${formatDate(app.installTime)}
                更新时间: ${formatDate(app.updateTime)}
                类型: ${if (app.isSystemApp) "系统应用" else "用户应用"}
            """.trimIndent())
            .setPositiveButton("打开") { _, _ ->
                IntentUtils.openApp(this, app.packageName)
            }
            .setNeutralButton("应用信息") { _, _ ->
                IntentUtils.openAppDetails(this, app.packageName)
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showAppOptions(app: AppUtils.AppInfo) {
        val options = mutableListOf("打开", "应用信息")
        
        if (!app.isSystemApp) {
            options.add("卸载")
        }
        
        options.add("添加到主屏幕")

        AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "打开" -> IntentUtils.openApp(this, app.packageName)
                    "应用信息" -> IntentUtils.openAppDetails(this, app.packageName)
                    "卸载" -> uninstallApp(app.packageName)
                    "添加到主屏幕" -> {
                        // Add to desktop
                        Toast.makeText(this, "功能开发中", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun uninstallApp(packageName: String) {
        AlertDialog.Builder(this)
            .setTitle("确认卸载")
            .setMessage("确定要卸载此应用吗?")
            .setPositiveButton("卸载") { _, _ ->
                IntentUtils.uninstallApp(this, packageName)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "未知"
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
