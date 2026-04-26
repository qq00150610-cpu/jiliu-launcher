package com.jiliu.launcher.ui.wallpaper

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ItemWallpaperBinding
import com.jiliu.launcher.model.WallpaperInfo

class WallpaperAdapter(
    private val onWallpaperClick: (WallpaperInfo) -> Unit
) : ListAdapter<WallpaperInfo, WallpaperAdapter.WallpaperViewHolder>(WallpaperDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperViewHolder {
        val binding = ItemWallpaperBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WallpaperViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WallpaperViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WallpaperViewHolder(
        private val binding: ItemWallpaperBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(wallpaper: WallpaperInfo) {
            binding.wallpaperImage.load(wallpaper.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.wallpaper_placeholder)
                error(R.drawable.wallpaper_placeholder)
            }
            
            binding.wallpaperTitle.text = wallpaper.title
            
            binding.root.setOnClickListener {
                onWallpaperClick(wallpaper)
            }
        }
    }

    class WallpaperDiffCallback : DiffUtil.ItemCallback<WallpaperInfo>() {
        override fun areItemsTheSame(oldItem: WallpaperInfo, newItem: WallpaperInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WallpaperInfo, newItem: WallpaperInfo): Boolean {
            return oldItem == newItem
        }
    }
}
