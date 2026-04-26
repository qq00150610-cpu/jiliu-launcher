package com.jiliu.launcher.model

/**
 * Wallpaper model
 */
data class WallpaperInfo(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val imageUrl: String,
    val author: String = "",
    val category: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val isFavorite: Boolean = false
)

/**
 * Wallpaper category
 */
data class WallpaperCategory(
    val id: String,
    val name: String,
    val icon: String = "",
    val count: Int = 0
)
