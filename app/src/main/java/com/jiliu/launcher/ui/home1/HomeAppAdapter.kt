package com.jiliu.launcher.ui.home1

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.databinding.ItemHomeAppBinding
import com.jiliu.launcher.util.AppUtils

class HomeAppAdapter(
    private val apps: List<AppUtils.AppInfo>,
    private val onAppClick: (AppUtils.AppInfo) -> Unit
) : RecyclerView.Adapter<HomeAppAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemHomeAppBinding.inflate(
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
        private val binding: ItemHomeAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppUtils.AppInfo) {
            binding.appIcon.setImageDrawable(app.icon)
            binding.appName.text = app.appName
            
            binding.root.setOnClickListener {
                onAppClick(app)
            }
            
            binding.root.setOnLongClickListener {
                // Long click to show app info
                onAppClick(app)
                true
            }
        }
    }
}
