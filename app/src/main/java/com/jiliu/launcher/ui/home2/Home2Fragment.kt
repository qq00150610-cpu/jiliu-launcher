package com.jiliu.launcher.ui.home2

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
import com.jiliu.launcher.databinding.FragmentHome2Binding
import com.jiliu.launcher.ui.main.MainActivity
import com.jiliu.launcher.util.AppUtils
import com.jiliu.launcher.util.IntentUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Home 2 Fragment - 车载桌面横屏版本
 * 左右布局：左侧地图弹窗(45%) + 右侧媒体播放(55%)
 */
class Home2Fragment : Fragment() {

    private var _binding: FragmentHome2Binding? = null
    private val binding get() = _binding!!

    private lateinit var appAdapter: Home2AppAdapter

    private val installedApps = mutableListOf<AppUtils.AppInfo>()
    
    private val handler = Handler(Looper.getMainLooper())
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000)
        }
    }

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
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        handler.post(timeUpdateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timeUpdateRunnable)
    }

    private fun setupUI() {
        // Setup app grid (如果需要显示应用网格的话)
        appAdapter = Home2AppAdapter(installedApps) { app ->
            IntentUtils.openApp(requireContext(), app.packageName)
        }

        // Top bar buttons
        binding.btnSettings.setOnClickListener {
            (activity as? MainActivity)?.openSettings()
        }

        binding.btnRefresh.setOnClickListener {
            loadApps()
        }

        binding.btnToggleMode.setOnClickListener {
            (activity as? MainActivity)?.toggleHomeMode()
        }

        binding.btnWallpaper.setOnClickListener {
            (activity as? MainActivity)?.openWallpaperPicker()
        }

        // Map card click
        binding.cardMap.setOnClickListener {
            openNavigation()
        }

        // Media card click
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

        binding.btnMusicShuffle.setOnClickListener {
            toggleShuffle()
        }

        binding.btnMusicRepeat.setOnClickListener {
            toggleRepeat()
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
            5
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
            "com.android.music",
            "com.google.android.apps.youtube.music",
            "com.spotify.music",
            "com.tencent.qqmusic"
        )
        
        for (packageName in musicPackages) {
            if (IntentUtils.isAppInstalled(requireContext(), packageName)) {
                IntentUtils.openApp(requireContext(), packageName)
                return
            }
        }
    }

    private fun toggleMusicPlayback() {
        val intent = Intent("com.android.music.musicservicecommand")
        intent.putExtra("command", "togglepause")
        requireContext().sendBroadcast(intent)
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

    private fun toggleShuffle() {
        // 切换随机播放
        val intent = Intent("com.android.music.musicservicecommand")
        intent.putExtra("command", "shuffle")
        requireContext().sendBroadcast(intent)
    }

    private fun toggleRepeat() {
        // 切换循环模式
        val intent = Intent("com.android.music.musicservicecommand")
        intent.putExtra("command", "repeat")
        requireContext().sendBroadcast(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timeUpdateRunnable)
        _binding = null
    }
}
