package com.jiliu.launcher.ui.home1

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.databinding.ItemAppQingBinding
import com.jiliu.launcher.util.AppUtils

/**
 * 氢桌面风格应用适配器
 * 特点：大图标、圆角卡片、简洁名称
 */
class HomeQingAppAdapter(
    private val apps: List<AppUtils.AppInfo>,
    private val onAppClick: (AppUtils.AppInfo) -> Unit
) : RecyclerView.Adapter<HomeQingAppAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppQingBinding.inflate(
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
        private val binding: ItemAppQingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppUtils.AppInfo) {
            binding.appName.text = app.appName
            binding.appIcon.setImageDrawable(app.icon)

            binding.root.setOnClickListener {
                onAppClick(app)
            }
        }
    }
}
