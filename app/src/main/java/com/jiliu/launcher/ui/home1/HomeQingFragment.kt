package com.jiliu.launcher.ui.home1

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiliu.launcher.App
import com.jiliu.launcher.databinding.FragmentHomeQingBinding
import com.jiliu.launcher.ui.main.MainActivity
import com.jiliu.launcher.util.AppUtils
import com.jiliu.launcher.util.IntentUtils

/**
 * 氢桌面风格首页 - 简洁、智能分类
 * 特点：无Dock栏、卡片式设计、智能文件夹分类、快捷搜索
 */
class HomeQingFragment : Fragment() {

    private var _binding: FragmentHomeQingBinding? = null
    private val binding get() = _binding!!

    private lateinit var appAdapter: HomeQingAppAdapter
    private lateinit var folderAdapter: FolderCardAdapter

    private val installedApps = mutableListOf<AppUtils.AppInfo>()
    private val smartFolders = mutableListOf<SmartFolder>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeQingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadApps()
        setupSmartFolders()
    }

    private fun setupUI() {
        // 设置应用网格 - 氢桌面风格：更大图标，更少列数
        appAdapter = HomeQingAppAdapter(installedApps) { app ->
            IntentUtils.openApp(requireContext(), app.packageName)
        }

        binding.appGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = appAdapter
        }

        // 设置智能文件夹网格
        folderAdapter = FolderCardAdapter(smartFolders) { folder ->
            openFolder(folder)
        }

        binding.folderGrid.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = folderAdapter
        }

        // 搜索栏点击
        binding.searchCard.setOnClickListener {
            showSearchDialog()
        }

        // 设置按钮
        binding.btnSettings.setOnClickListener {
            (activity as? MainActivity)?.openSettings()
        }

        // 壁纸按钮
        binding.btnWallpaper.setOnClickListener {
            (activity as? MainActivity)?.openWallpaper()
        }

        // 添加快捷方式
        binding.btnAddShortcut.setOnClickListener {
            showAddShortcutDialog()
        }

        // 自动分类
        binding.btnAutoClassify.setOnClickListener {
            autoClassifyApps()
        }

        // 全部应用
        binding.btnAllApps.setOnClickListener {
            (activity as? MainActivity)?.openAppManager()
        }
    }

    private fun loadApps() {
        installedApps.clear()
        installedApps.addAll(
            AppUtils.getInstalledApps(requireContext(), includeSystemApps = false)
                .sortedBy { it.appName }
        )
        appAdapter.notifyDataSetChanged()
    }

    private fun setupSmartFolders() {
        // 初始化智能文件夹 - 按应用类型自动分类
        smartFolders.clear()

        // 导航分类
        val navApps = installedApps.filter {
            it.packageName.contains("map") ||
            it.packageName.contains("nav") ||
            it.appName.contains("地图") ||
            it.appName.contains("导航")
        }
        if (navApps.isNotEmpty()) {
            smartFolders.add(SmartFolder("导航", navApps.take(4), "ic_nav"))
        }

        // 音乐分类
        val musicApps = installedApps.filter {
            it.packageName.contains("music") ||
            it.packageName.contains("audio") ||
            it.appName.contains("音乐") ||
            it.appName.contains("播放器")
        }
        if (musicApps.isNotEmpty()) {
            smartFolders.add(SmartFolder("音乐", musicApps.take(4), "ic_music"))
        }

        // 视频分类
        val videoApps = installedApps.filter {
            it.packageName.contains("video") ||
            it.packageName.contains("player") ||
            it.appName.contains("视频") ||
            it.appName.contains("影视")
        }
        if (videoApps.isNotEmpty()) {
            smartFolders.add(SmartFolder("视频", videoApps.take(4), "ic_video"))
        }

        folderAdapter.notifyDataSetChanged()
    }

    private fun autoClassifyApps() {
        // 重新分类应用
        setupSmartFolders()
        loadApps()
    }

    private fun openFolder(folder: SmartFolder) {
        // 打开智能文件夹
        val intent = Intent(Intent.ACTION_VIEW).apply {
            putExtra("folder_apps", folder.apps.map { it.packageName }.toTypedArray())
            setClassName(requireContext().packageName, "${requireContext().packageName}.ui.folder.FolderActivity")
        }
        startActivity(intent)
    }

    private fun showSearchDialog() {
        // 显示搜索对话框
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(requireContext().packageName, "${requireContext().packageName}.ui.search.SearchActivity")
        }
        startActivity(intent)
    }

    private fun showAddShortcutDialog() {
        // 显示添加快捷方式对话框
        (activity as? MainActivity)?.openAppManager()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
        setupSmartFolders()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * 智能文件夹数据类
 */
data class SmartFolder(
    val name: String,
    val apps: List<AppUtils.AppInfo>,
    val iconType: String
)
