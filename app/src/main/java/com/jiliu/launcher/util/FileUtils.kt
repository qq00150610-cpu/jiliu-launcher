package com.jiliu.launcher.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.text.TextUtils
import java.io.File

object FileUtils {

    /**
     * Get USB storage paths
     */
    fun getUsbStoragePaths(context: Context): List<String> {
        val usbPaths = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            try {
                val storageVolumes = StorageManager.getStorageVolumes(context)
                for (volume in storageVolumes) {
                    if (volume.isUsb) {
                        val path = volume.directory?.absolutePath
                        if (!path.isNullOrEmpty() && File(path).exists()) {
                            usbPaths.add(path)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Alternative: scan common USB paths
        val commonUsbPaths = listOf(
            "/storage/usb0",
            "/storage/usb1",
            "/storage/usb2",
            "/mnt/usb0",
            "/mnt/usb1",
            "/mnt/usbhost0",
            "/mnt/usbhost1"
        )
        
        for (path in commonUsbPaths) {
            val file = File(path)
            if (file.exists() && file.canRead() && !usbPaths.contains(path)) {
                usbPaths.add(path)
            }
        }
        
        return usbPaths
    }

    /**
     * Get external storage path
     */
    fun getExternalStoragePath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    /**
     * Get all storage paths (internal + external + USB)
     */
    fun getAllStoragePaths(context: Context): List<StorageInfo> {
        val storages = mutableListOf<StorageInfo>()
        
        // Internal storage
        val internalPath = Environment.getExternalStorageDirectory().absolutePath
        storages.add(
            StorageInfo(
                name = "内部存储",
                path = internalPath,
                type = StorageType.INTERNAL
            )
        )
        
        // External storage (SD card)
        val externalPath = System.getenv("EXTERNAL_STORAGE")
        if (!externalPath.isNullOrEmpty() && externalPath != internalPath) {
            val externalFile = File(externalPath)
            if (externalFile.exists() && externalFile.canRead()) {
                storages.add(
                    StorageInfo(
                        name = "SD卡",
                        path = externalPath,
                        type = StorageType.EXTERNAL
                    )
                )
            }
        }
        
        // USB storages
        for (usbPath in getUsbStoragePaths(context)) {
            if (usbPath != internalPath) {
                storages.add(
                    StorageInfo(
                        name = "USB存储",
                        path = usbPath,
                        type = StorageType.USB
                    )
                )
            }
        }
        
        return storages
    }

    /**
     * Get files in directory
     */
    fun getFilesInDirectory(path: String): List<FileItem> {
        val files = mutableListOf<FileItem>()
        val directory = File(path)
        
        if (!directory.exists() || !directory.isDirectory || !directory.canRead()) {
            return files
        }
        
        val listFiles = directory.listFiles() ?: return files
        
        for (file in listFiles) {
            files.add(
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    extension = if (file.isFile) getFileExtension(file.name) else null
                )
            )
        }
        
        // Sort: directories first, then by name
        return files.sortedWith(
            compareBy<FileItem> { !it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Get file extension
     */
    fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0) fileName.substring(lastDot + 1).lowercase() else ""
    }

    /**
     * Get MIME type from extension
     */
    fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image/*"
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp" -> "video/*"
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma" -> "audio/*"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "txt", "log" -> "text/plain"
            "zip", "rar", "7z", "tar", "gz" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
    }

    /**
     * Format file size
     */
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        
        return String.format(
            "%.2f %s",
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    /**
     * Get category type for file
     */
    fun getFileCategory(extension: String): FileCategory {
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg" -> FileCategory.IMAGE
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp", "webm" -> FileCategory.VIDEO
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "ape" -> FileCategory.AUDIO
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "rtf" -> FileCategory.DOCUMENT
            "zip", "rar", "7z", "tar", "gz", "bz2" -> FileCategory.ARCHIVE
            "apk" -> FileCategory.APP
            else -> FileCategory.OTHER
        }
    }

    /**
     * Check if file is media
     */
    fun isMediaFile(extension: String): Boolean {
        val category = getFileCategory(extension)
        return category == FileCategory.IMAGE || 
               category == FileCategory.VIDEO || 
               category == FileCategory.AUDIO
    }

    /**
     * Check if file is video
     */
    fun isVideoFile(extension: String): Boolean {
        return getFileCategory(extension) == FileCategory.VIDEO
    }

    /**
     * Check if file is audio
     */
    fun isAudioFile(extension: String): Boolean {
        return getFileCategory(extension) == FileCategory.AUDIO
    }

    /**
     * Check if file is image
     */
    fun isImageFile(extension: String): Boolean {
        return getFileCategory(extension) == FileCategory.IMAGE
    }

    /**
     * Delete file or directory
     */
    fun deleteFile(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Copy file
     */
    fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val source = File(sourcePath)
            val dest = File(destPath)
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Move file
     */
    fun moveFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val source = File(sourcePath)
            val dest = File(destPath)
            source.renameTo(dest)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Create directory
     */
    fun createDirectory(path: String): Boolean {
        return try {
            File(path).mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Rename file
     */
    fun renameFile(oldPath: String, newName: String): Boolean {
        return try {
            val oldFile = File(oldPath)
            val newFile = File(oldFile.parent, newName)
            oldFile.renameTo(newFile)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    data class FileItem(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
        val extension: String? = null
    ) {
        val category: FileCategory
            get() = if (isDirectory) FileCategory.FOLDER 
                    else FileCategory.getByExtension(extension ?: "")
    }

    data class StorageInfo(
        val name: String,
        val path: String,
        val type: StorageType
    )

    enum class StorageType {
        INTERNAL,
        EXTERNAL,
        USB
    }

    enum class FileCategory {
        FOLDER,
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT,
        ARCHIVE,
        APP,
        OTHER;

        companion object {
            fun getByExtension(extension: String): FileCategory {
                return when (extension.lowercase()) {
                    "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg" -> IMAGE
                    "mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp", "webm" -> VIDEO
                    "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "ape" -> AUDIO
                    "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt", "rtf" -> DOCUMENT
                    "zip", "rar", "7z", "tar", "gz", "bz2" -> ARCHIVE
                    "apk" -> APP
                    else -> OTHER
                }
            }
        }
    }
}
