package com.jiliu.launcher.ui.home1

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.jiliu.launcher.App
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.FragmentHome1Binding
import com.jiliu.launcher.ui.main.MainActivity
import com.jiliu.launcher.util.AppUtils
import com.jiliu.launcher.util.IntentUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Home 1 Fragment - 车载桌面竖屏版本
 * 包含：状态栏、地图弹窗、媒体播放、应用网格
 */
class Home1Fragment : Fragment() {

    private var _binding: FragmentHome1Binding? = null
    private val binding get() = _binding!!

    private lateinit var appAdapter: HomeAppAdapter

    private val installedApps = mutableListOf<AppUtils.AppInfo>()
    
    private val handler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000) // 每秒更新
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHome1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        // 开始更新时间
        handler.post(timeUpdateRunnable)
    }

    override fun onPause() {
        super.onPause()
        // 停止更新
        handler.removeCallbacks(timeUpdateRunnable)
    }

    private fun setupUI() {
        // Setup app grid
        appAdapter = HomeAppAdapter(installedApps) { app ->
            IntentUtils.openApp(requireContext(), app.packageName)
        }

        binding.appGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), getColumnCount())
            adapter = appAdapter
        }

        // Setup top bar buttons
        binding.btnSettings.setOnClickListener {
            (activity as? MainActivity)?.openSettings()
        }

        // Setup quick action buttons
        binding.btnRefresh.setOnClickListener {
            loadApps()
        }

        binding.btnToggleMode.setOnClickListener {
            (activity as? MainActivity)?.toggleHomeMode()
        }

        binding.btnWallpaper.setOnClickListener {
            (activity as? MainActivity)?.openWallpaperPicker()
        }

        // Map card click - open navigation
        binding.cardMap.setOnClickListener {
            openNavigation()
        }

        // Media card click - open music player
        binding.cardMedia.setOnClickListener {
            openMusicPlayer()
        }

        // Music control buttons
        binding.btnMusicPlay.setOnClickListener {
            toggleMusicPlayback()
        }

        binding.btnMusicPrev.setOnClickListener {
            previousTrack()
        }

        binding.btnMusicNext.setOnClickListener {
            nextTrack()
        }

        // User avatar click
        binding.userAvatar.setOnClickListener {
            (activity as? MainActivity)?.openSettings()
        }

        // Initial time/date update
        updateDateTime()
    }

    private fun loadApps() {
        installedApps.clear()
        installedApps.addAll(
            AppUtils.getInstalledApps(requireContext(), includeSystemApps = false)
        )
        appAdapter.notifyDataSetChanged()
    }

    private fun getColumnCount(): Int {
        // 横屏时增加列数
        val orientation = resources.configuration.orientation
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            6
        } else {
            App.instance.preferencesManager.gridColumns
        }
    }

    private fun updateDateTime() {
        val calendar = Calendar.getInstance()
        
        // 更新时间
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.textTime.text = timeFormat.format(calendar.time)
        
        // 更新日期
        val dateFormat = SimpleDateFormat("yyyy年M月d日 E", Locale.CHINESE)
        binding.textDate.text = dateFormat.format(calendar.time)
    }

    private fun openNavigation() {
        // 尝试打开导航应用
        val navPackages = listOf(
            "com.autonavi.minimap",      // 高德地图
            "com.baidu.BaiduMap",        // 百度地图
            "com.google.android.apps.maps", // Google Maps
            "com.tencent.map"            // 腾讯地图
        )
        
        for (packageName in navPackages) {
            if (IntentUtils.isAppInstalled(requireContext(), packageName)) {
                IntentUtils.openApp(requireContext(), packageName)
                return
            }
        }
        
        // 没有导航应用，尝试打开地图
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("geo:0,0?q=*")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // 忽略
        }
    }

    private fun openMusicPlayer() {
        // 尝试打开音乐应用
        val musicPackages = listOf(
            "com.android.music",              // 系统音乐
            "com.google.android.apps.youtube.music", // YouTube Music
            "com.spotify.music",             // Spotify
            "com.tencent.qqmusic"            // QQ音乐
        )
        
        for (packageName in musicPackages) {
            if (IntentUtils.isAppInstalled(requireContext(), packageName)) {
                IntentUtils.openApp(requireContext(), packageName)
                return
            }
        }
    }

    private fun toggleMusicPlayback() {
        // 发送播放/暂停广播
        val intent = Intent("com.android.music.musicservicecommand")
        intent.putExtra("command", "togglepause")
        requireContext().sendBroadcast(intent)
        
        // 更新按钮图标
        // 这里可以添加状态切换逻辑
    }

    private fun previousTrack() {
        val intent = Intent("com.android.music.musicservicecommand")
        intent.putExtra("command", "previous")
        requireContext().sendBroadcast(intent)
    }

    private fun nextTrack() {
        val intent = Intent("com.android.music.musicservicecommand")
        intent.putExtra("command", "next")
        requireContext().sendBroadcast(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timeUpdateRunnable)
        _binding = null
    }
}
