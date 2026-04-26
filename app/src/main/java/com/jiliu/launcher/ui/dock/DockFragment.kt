package com.jiliu.launcher.ui.dock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiliu.launcher.App
import com.jiliu.launcher.databinding.FragmentDockBinding
import com.jiliu.launcher.util.AppUtils
import com.jiliu.launcher.util.IntentUtils

class DockFragment : Fragment() {

    private var _binding: FragmentDockBinding? = null
    private val binding get() = _binding!!

    private lateinit var dockAdapter: DockAdapter

    private val dockApps = mutableListOf<AppUtils.AppInfo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        loadDockApps()
    }

    private fun setupUI() {
        dockAdapter = DockAdapter(dockApps) { app ->
            IntentUtils.openApp(requireContext(), app.packageName)
        }

        binding.dockRecycler.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = dockAdapter
        }
    }

    private fun loadDockApps() {
        dockApps.clear()
        
        val dockAppsString = App.instance.preferencesManager.dockApps
        val packageNames = dockAppsString.split(",").filter { it.isNotBlank() }
        
        for (packageName in packageNames) {
            val appInfo = getAppInfo(packageName)
            if (appInfo != null) {
                dockApps.add(appInfo)
            }
        }
        
        // If no dock apps configured, use defaults
        if (dockApps.isEmpty()) {
            val defaultPackages = listOf(
                "com.android.settings",
                "com.android.chrome",
                "com.google.android.youtube",
                "com.android.music"
            )
            for (packageName in defaultPackages) {
                val appInfo = getAppInfo(packageName)
                if (appInfo != null) {
                    dockApps.add(appInfo)
                }
            }
        }
        
        dockAdapter.notifyDataSetChanged()
    }

    private fun getAppInfo(packageName: String): AppUtils.AppInfo? {
        return try {
            val pm = requireContext().packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            AppUtils.AppInfo(
                packageName = appInfo.packageName,
                appName = pm.getApplicationLabel(appInfo).toString(),
                icon = appInfo.loadIcon(pm),
                isSystemApp = AppUtils.isSystemApp(appInfo)
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
