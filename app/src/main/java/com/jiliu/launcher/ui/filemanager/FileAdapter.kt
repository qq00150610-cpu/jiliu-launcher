package com.jiliu.launcher.ui.filemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ItemFileBinding
import com.jiliu.launcher.util.FileUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileAdapter(
    private val onItemClick: (FileUtils.FileItem) -> Unit,
    private val onItemLongClick: (FileUtils.FileItem) -> Unit,
    private val isSelectedMode: () -> Boolean,
    private val isSelected: (String) -> Boolean,
    private val onSelectionChange: (String) -> Unit
) : ListAdapter<FileUtils.FileItem, FileAdapter.FileViewHolder>(FileDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FileViewHolder(
        private val binding: ItemFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fileItem: FileUtils.FileItem) {
            binding.fileName.text = fileItem.name
            
            if (fileItem.isDirectory) {
                binding.fileIcon.setImageResource(R.drawable.ic_folder)
                binding.fileSize.text = ""
                binding.fileInfo.text = getFolderInfo(fileItem)
            } else {
                binding.fileIcon.setImageResource(getFileIcon(fileItem.extension ?: ""))
                binding.fileSize.text = FileUtils.formatFileSize(fileItem.size)
                binding.fileInfo.text = dateFormat.format(Date(fileItem.lastModified))
            }

            // Selection mode
            val inSelectionMode = isSelectedMode()
            binding.checkBox.visibility = if (inSelectionMode) View.VISIBLE else View.GONE
            binding.checkBox.isChecked = isSelected(fileItem.path)

            binding.root.setOnClickListener {
                if (inSelectionMode) {
                    onSelectionChange(fileItem.path)
                } else {
                    onItemClick(fileItem)
                }
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(fileItem)
                true
            }
        }

        private fun getFolderInfo(fileItem: FileUtils.FileItem): String {
            // Count items in folder
            return try {
                val folder = java.io.File(fileItem.path)
                val count = folder.listFiles()?.size ?: 0
                "$count 个项目"
            } catch (e: Exception) {
                ""
            }
        }

        private fun getFileIcon(extension: String): Int {
            return when (FileUtils.getFileCategory(extension)) {
                FileUtils.FileCategory.IMAGE -> R.drawable.ic_image
                FileUtils.FileCategory.VIDEO -> R.drawable.ic_video
                FileUtils.FileCategory.AUDIO -> R.drawable.ic_audio
                FileUtils.FileCategory.DOCUMENT -> R.drawable.ic_document
                FileUtils.FileCategory.ARCHIVE -> R.drawable.ic_archive
                FileUtils.FileCategory.APP -> R.drawable.ic_app
                else -> R.drawable.ic_file
            }
        }
    }

    class FileDiffCallback : DiffUtil.ItemCallback<FileUtils.FileItem>() {
        override fun areItemsTheSame(oldItem: FileUtils.FileItem, newItem: FileUtils.FileItem): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: FileUtils.FileItem, newItem: FileUtils.FileItem): Boolean {
            return oldItem == newItem
        }
    }
}
