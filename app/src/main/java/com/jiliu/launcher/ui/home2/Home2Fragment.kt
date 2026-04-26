package com.jiliu.launcher.ui.home2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiliu.launcher.App
import com.jiliu.launcher.databinding.FragmentHome2Binding
import com.jiliu.launcher.ui.main.MainActivity
import com.jiliu.launcher.ui.player.PlayerActivity
import com.jiliu.launcher.util.AppUtils
import com.jiliu.launcher.util.IntentUtils

/**
 * Home 2 Fragment - Rich mode
 * More widgets, shortcuts, and customization options
 */
class Home2Fragment : Fragment() {

    private var _binding: FragmentHome2Binding? = null
    private val binding get() = _binding!!

    private lateinit var appAdapter: Home2AppAdapter
    private lateinit var shortcutAdapter: ShortcutAdapter

    private val installedApps = mutableListOf<AppUtils.AppInfo>()
    private val shortcuts = mutableListOf<ShortcutItem>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHome2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadData()
    }

    private fun setupUI() {
        // Setup app grid
        appAdapter = Home2AppAdapter(installedApps) { app ->
            IntentUtils.openApp(requireContext(), app.packageName)
        }

        binding.appGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), getColumnCount())
            adapter = appAdapter
        }

        // Setup shortcuts
        shortcuts.addAll(getDefaultShortcuts())
        shortcutAdapter = ShortcutAdapter(shortcuts) { shortcut ->
            handleShortcutClick(shortcut)
        }

        binding.shortcutsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = shortcutAdapter
        }

        // Setup quick action buttons
        binding.btnSettings.setOnClickListener {
            (activity as? MainActivity)?.openSettings()
        }

        binding.btnFileManager.setOnClickListener {
            (activity as? MainActivity)?.openFileManager()
        }

        binding.btnAppManager.setOnClickListener {
            (activity as? MainActivity)?.openAppManager()
        }

        binding.btnWallpaper.setOnClickListener {
            (activity as? MainActivity)?.openWallpaper()
        }

        binding.btnMember.setOnClickListener {
            (activity as? MainActivity)?.openMember()
        }

        binding.btnToggleMode.setOnClickListener {
            (activity as? MainActivity)?.toggleHomeMode()
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            loadData()
        }

        // Music mini player
        binding.musicMiniPlayer.setOnClickListener {
            startActivity(Intent(requireContext(), PlayerActivity::class.java))
        }
    }

    private fun loadData() {
        installedApps.clear()
        installedApps.addAll(
            AppUtils.getInstalledApps(requireContext(), includeSystemApps = false)
        )
        appAdapter.notifyDataSetChanged()
    }

    private fun getColumnCount(): Int {
        return App.instance.preferencesManager.gridColumns
    }

    private fun getDefaultShortcuts(): List<ShortcutItem> {
        return listOf(
            ShortcutItem("settings", "设置", "ic_settings"),
            ShortcutItem("filemanager", "文件管理", "ic_folder"),
            ShortcutItem("music", "音乐", "ic_music"),
            ShortcutItem("video", "视频", "ic_video"),
            ShortcutItem("browser", "浏览器", "ic_browser"),
            ShortcutItem("camera", "相机", "ic_camera"),
            ShortcutItem("maps", "地图", "ic_maps"),
            ShortcutItem("weather", "天气", "ic_weather")
        )
    }

    private fun handleShortcutClick(shortcut: ShortcutItem) {
        when (shortcut.id) {
            "settings" -> (activity as? MainActivity)?.openSettings()
            "filemanager" -> (activity as? MainActivity)?.openFileManager()
            "appmanager" -> (activity as? MainActivity)?.openAppManager()
            "wallpaper" -> (activity as? MainActivity)?.openWallpaper()
            "member" -> (activity as? MainActivity)?.openMember()
            "music", "video" -> startActivity(Intent(requireContext(), PlayerActivity::class.java))
            else -> IntentUtils.openApp(requireContext(), shortcut.id)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class ShortcutItem(
        val id: String,
        val title: String,
        val iconRes: String
    )
}
