package com.jiliu.launcher.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.jiliu.launcher.model.WallpaperCategory
import com.jiliu.launcher.model.WallpaperInfo
import com.jiliu.launcher.util.WallpaperUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class WallpaperRepository(private val context: Context) {

    /**
     * Get wallpaper categories (mock implementation)
     */
    suspend fun getCategories(): List<WallpaperCategory> = withContext(Dispatchers.IO) {
        // Mock categories - in production, fetch from API
        listOf(
            WallpaperCategory("1", "风景", "ic_landscape", 50),
            WallpaperCategory("2", "城市", "ic_city", 30),
            WallpaperCategory("3", "自然", "ic_nature", 40),
            WallpaperCategory("4", "抽象", "ic_abstract", 25),
            WallpaperCategory("5", "汽车", "ic_car", 35),
            WallpaperCategory("6", "动物", "ic_animal", 20),
            WallpaperCategory("7", "美女", "ic_beauty", 45),
            WallpaperCategory("8", "动漫", "ic_anime", 55),
            WallpaperCategory("9", "影视", "ic_movie", 30),
            WallpaperCategory("10", "节日", "ic_festival", 15)
        )
    }

    /**
     * Get wallpapers by category (mock implementation)
     */
    suspend fun getWallpapersByCategory(categoryId: String): List<WallpaperInfo> = withContext(Dispatchers.IO) {
        // Mock wallpapers - in production, fetch from API
        val wallpapers = mutableListOf<WallpaperInfo>()
        
        for (i in 1..20) {
            wallpapers.add(
                WallpaperInfo(
                    id = "${categoryId}_$i",
                    title = "壁纸 $i",
                    thumbnailUrl = "https://via.placeholder.com/300x500/1A1A1A/D4AF37?text=Wallpaper$i",
                    imageUrl = "https://via.placeholder.com/1080x1920/1A1A1A/D4AF37?text=Wallpaper$i",
                    author = "极流官方",
                    category = categoryId,
                    width = 1080,
                    height = 1920
                )
            )
        }
        
        wallpapers
    }

    /**
     * Get featured wallpapers (mock implementation)
     */
    suspend fun getFeaturedWallpapers(): List<WallpaperInfo> = withContext(Dispatchers.IO) {
        val wallpapers = mutableListOf<WallpaperInfo>()
        
        for (i in 1..10) {
            wallpapers.add(
                WallpaperInfo(
                    id = "featured_$i",
                    title = "精选壁纸 $i",
                    thumbnailUrl = "https://via.placeholder.com/300x500/C41E3A/FFFFFF?text=Featured$i",
                    imageUrl = "https://via.placeholder.com/1080x1920/C41E3A/FFFFFF?text=Featured$i",
                    author = "极流官方",
                    category = "featured",
                    width = 1080,
                    height = 1920
                )
            )
        }
        
        wallpapers
    }

    /**
     * Download wallpaper
     */
    suspend fun downloadWallpaper(wallpaper: WallpaperInfo): File? = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "wallpapers")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val file = File(cacheDir, "${wallpaper.id}.jpg")
            
            // In production, use proper image loading library
            // For now, create a placeholder file
            if (!file.exists()) {
                file.createNewFile()
            }
            
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Set wallpaper from URL
     */
    suspend fun setWallpaperFromUrl(url: String, target: WallpaperUtils.WallpaperTarget): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "wallpapers")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                
                val file = File(cacheDir, "temp_wallpaper.jpg")
                
                // Download image
                URL(url).openStream().use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Set as wallpaper
                WallpaperUtils.setWallpaperFromFile(context, file.absolutePath, target)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Set wallpaper from file
     */
    suspend fun setWallpaperFromFile(filePath: String, target: WallpaperUtils.WallpaperTarget): Boolean {
        return withContext(Dispatchers.IO) {
            WallpaperUtils.setWallpaperFromFile(context, filePath, target)
        }
    }

    /**
     * Get downloaded wallpapers
     */
    suspend fun getDownloadedWallpapers(): List<File> = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "wallpapers")
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * Delete downloaded wallpaper
     */
    suspend fun deleteWallpaper(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
