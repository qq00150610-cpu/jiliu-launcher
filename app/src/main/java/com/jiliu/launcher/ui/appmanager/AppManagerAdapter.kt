package com.jiliu.launcher.ui.appmanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.databinding.ItemAppBinding
import com.jiliu.launcher.util.AppUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppManagerAdapter(
    private val onAppClick: (AppUtils.AppInfo) -> Unit,
    private val onAppLongClick: (AppUtils.AppInfo) -> Unit
) : ListAdapter<AppUtils.AppInfo, AppManagerAdapter.AppViewHolder>(AppDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppUtils.AppInfo) {
            binding.appIcon.setImageDrawable(app.icon)
            binding.appName.text = app.appName
            binding.appPackage.text = app.packageName
            binding.appVersion.text = app.versionName ?: ""
            
            binding.appType.text = if (app.isSystemApp) "系统" else "用户"
            
            binding.root.setOnClickListener {
                onAppClick(app)
            }
            
            binding.root.setOnLongClickListener {
                onAppLongClick(app)
                true
            }
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppUtils.AppInfo>() {
        override fun areItemsTheSame(oldItem: AppUtils.AppInfo, newItem: AppUtils.AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppUtils.AppInfo, newItem: AppUtils.AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}
