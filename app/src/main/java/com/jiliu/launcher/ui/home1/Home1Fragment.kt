package com.jiliu.launcher.ui.home1

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.jiliu.launcher.App
import com.jiliu.launcher.databinding.FragmentHome1Binding
import com.jiliu.launcher.ui.main.MainActivity
import com.jiliu.launcher.util.AppUtils
import com.jiliu.launcher.util.IntentUtils

/**
 * Home 1 Fragment - Simple mode
 * Clean, minimal layout with essential apps
 */
class Home1Fragment : Fragment() {

    private var _binding: FragmentHome1Binding? = null
    private val binding get() = _binding!!

    private lateinit var appAdapter: HomeAppAdapter

    private val installedApps = mutableListOf<AppUtils.AppInfo>()

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

    private fun setupUI() {
        // Setup app grid
        appAdapter = HomeAppAdapter(installedApps) { app ->
            IntentUtils.openApp(requireContext(), app.packageName)
        }

        binding.appGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), getColumnCount())
            adapter = appAdapter
        }

        // Setup quick action buttons
        binding.btnSettings.setOnClickListener {
            (activity as? MainActivity)?.openSettings()
        }

        binding.btnFileManager.setOnClickListener {
            (activity as? MainActivity)?.openFileManager()
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

        // Setup date/time display
        updateDateTime()
        
        // Refresh button
        binding.btnRefresh.setOnClickListener {
            loadApps()
        }
    }

    private fun loadApps() {
        installedApps.clear()
        installedApps.addAll(
            AppUtils.getInstalledApps(requireContext(), includeSystemApps = false)
        )
        appAdapter.notifyDataSetChanged()
    }

    private fun getColumnCount(): Int {
        return App.instance.preferencesManager.gridColumns
    }

    private fun updateDateTime() {
        // Update time in layout if needed
        // Using TextClock in XML is more efficient
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
