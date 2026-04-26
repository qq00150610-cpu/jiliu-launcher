package com.jiliu.launcher.ui.wallpaper

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.databinding.ItemWallpaperCategoryBinding
import com.jiliu.launcher.model.WallpaperCategory

class WallpaperCategoryAdapter(
    private val onCategoryClick: (WallpaperCategory) -> Unit
) : ListAdapter<WallpaperCategory, WallpaperCategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemWallpaperCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemWallpaperCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: WallpaperCategory) {
            binding.categoryName.text = category.name
            binding.categoryCount.text = "${category.count}张"
            
            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<WallpaperCategory>() {
        override fun areItemsTheSame(oldItem: WallpaperCategory, newItem: WallpaperCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WallpaperCategory, newItem: WallpaperCategory): Boolean {
            return oldItem == newItem
        }
    }
}
