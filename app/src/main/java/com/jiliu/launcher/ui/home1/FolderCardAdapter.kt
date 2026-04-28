package com.jiliu.launcher.ui.home1

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ItemFolderCardBinding
import com.jiliu.launcher.util.AppUtils

/**
 * 智能文件夹卡片适配器
 * 特点：卡片式设计、显示文件夹名称和预览应用
 */
class FolderCardAdapter(
    private val folders: List<SmartFolder>,
    private val onFolderClick: (SmartFolder) -> Unit
) : RecyclerView.Adapter<FolderCardAdapter.FolderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount(): Int = folders.size

    inner class FolderViewHolder(
        private val binding: ItemFolderCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: SmartFolder) {
            binding.folderName.text = folder.name
            binding.appCount.text = folder.apps.size.toString()

            // 设置文件夹图标
            val iconRes = when (folder.iconType) {
                "ic_nav" -> R.drawable.ic_nav
                "ic_music" -> R.drawable.ic_music
                "ic_video" -> R.drawable.ic_video
                else -> R.drawable.ic_folder
            }
            binding.folderIcon.setImageResource(iconRes)

            binding.root.setOnClickListener {
                onFolderClick(folder)
            }
        }
    }
}
