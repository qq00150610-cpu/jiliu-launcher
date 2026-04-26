package com.jiliu.launcher.ui.wallpaper

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ItemFeaturedWallpaperBinding
import com.jiliu.launcher.model.WallpaperInfo

class FeaturedWallpaperAdapter(
    private val wallpapers: List<WallpaperInfo>
) : RecyclerView.Adapter<FeaturedWallpaperAdapter.FeaturedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedViewHolder {
        val binding = ItemFeaturedWallpaperBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeaturedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeaturedViewHolder, position: Int) {
        holder.bind(wallpapers[position])
    }

    override fun getItemCount(): Int = wallpapers.size

    inner class FeaturedViewHolder(
        private val binding: ItemFeaturedWallpaperBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(wallpaper: WallpaperInfo) {
            binding.featuredImage.load(wallpaper.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.wallpaper_placeholder)
            }
            
            binding.featuredTitle.text = wallpaper.title
            binding.featuredAuthor.text = wallpaper.author
        }
    }

    fun submitList(newList: List<WallpaperInfo>) {
        // Update adapter data
    }
}
