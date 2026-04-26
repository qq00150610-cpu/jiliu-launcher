package com.jiliu.launcher.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.app.WallpaperManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object WallpaperUtils {

    /**
     * Set wallpaper from bitmap
     */
    suspend fun setWallpaper(
        context: Context,
        bitmap: Bitmap,
        target: WallpaperTarget = WallpaperTarget.HOME
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            // Scale bitmap to screen size
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, screenWidth, screenHeight, true)
            
            when (target) {
                WallpaperTarget.HOME -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(scaledBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    } else {
                        wallpaperManager.setBitmap(scaledBitmap)
                    }
                }
                WallpaperTarget.LOCK -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(scaledBitmap, null, true, WallpaperManager.FLAG_LOCK)
                    } else {
                        wallpaperManager.setBitmap(scaledBitmap)
                    }
                }
                WallpaperTarget.BOTH -> {
                    wallpaperManager.setBitmap(scaledBitmap)
                }
            }
            
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Set wallpaper from file path
     */
    suspend fun setWallpaperFromFile(
        context: Context,
        filePath: String,
        target: WallpaperTarget = WallpaperTarget.HOME
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(filePath)
            if (bitmap != null) {
                val result = setWallpaper(context, bitmap, target)
                bitmap.recycle()
                result
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Save wallpaper to gallery
     */
    suspend fun saveWallpaperToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "wallpaper_${System.currentTimeMillis()}"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val outputStream: OutputStream?
            val savedPath: String
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/JiLiuWallpaper")
                }
                
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                if (uri == null) return@withContext null
                
                outputStream = context.contentResolver.openOutputStream(uri)
                savedPath = uri.toString()
            } else {
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "JiLiuWallpaper"
                )
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                
                val file = File(directory, "$fileName.jpg")
                outputStream = FileOutputStream(file)
                savedPath = file.absolutePath
            }
            
            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            
            savedPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get current wallpaper bitmap
     */
    suspend fun getCurrentWallpaper(context: Context): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                wallpaperManager.drawable?.let { drawable ->
                    if (drawable is android.graphics.drawable.BitmapDrawable) {
                        return@withContext drawable.bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Clear wallpaper
     */
    suspend fun clearWallpaper(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.clear()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    enum class WallpaperTarget {
        HOME,
        LOCK,
        BOTH
    }
}
