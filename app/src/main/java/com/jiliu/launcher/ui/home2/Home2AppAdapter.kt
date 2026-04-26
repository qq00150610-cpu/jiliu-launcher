package com.jiliu.launcher.ui.home2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.databinding.ItemHome2AppBinding
import com.jiliu.launcher.util.AppUtils

class Home2AppAdapter(
    private val apps: List<AppUtils.AppInfo>,
    private val onAppClick: (AppUtils.AppInfo) -> Unit
) : RecyclerView.Adapter<Home2AppAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemHome2AppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(
        private val binding: ItemHome2AppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppUtils.AppInfo) {
            binding.appIcon.setImageDrawable(app.icon)
            binding.appName.text = app.appName
            
            binding.root.setOnClickListener {
                onAppClick(app)
            }
            
            binding.root.setOnLongClickListener {
                // Could show app options menu
                onAppClick(app)
                true
            }
        }
    }
}
