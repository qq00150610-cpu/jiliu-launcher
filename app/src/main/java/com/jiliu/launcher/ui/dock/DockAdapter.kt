package com.jiliu.launcher.ui.dock

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.databinding.ItemDockAppBinding
import com.jiliu.launcher.util.AppUtils

class DockAdapter(
    private val apps: List<AppUtils.AppInfo>,
    private val onAppClick: (AppUtils.AppInfo) -> Unit
) : RecyclerView.Adapter<DockAdapter.DockViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DockViewHolder {
        val binding = ItemDockAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DockViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class DockViewHolder(
        private val binding: ItemDockAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppUtils.AppInfo) {
            binding.dockIcon.setImageDrawable(app.icon)
            
            binding.root.setOnClickListener {
                onAppClick(app)
            }
        }
    }
}
