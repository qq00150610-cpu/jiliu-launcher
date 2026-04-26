package com.jiliu.launcher.model

import android.graphics.drawable.Drawable

/**
 * Desktop app icon model
 */
data class DesktopItem(
    val id: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val x: Int = 0,
    val y: Int = 0,
    val cellX: Int = 0,
    val cellY: Int = 0,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val container: Int = CONTAINER_DESKTOP,
    val isFolder: Boolean = false,
    val folderItems: List<DesktopItem> = emptyList()
) {
    companion object {
        const val CONTAINER_DESKTOP = 0
        const val CONTAINER_DOCK = 1
        const val CONTAINER_FOLDER = 2
    }
}

/**
 * Widget model
 */
data class WidgetInfo(
    val id: Long,
    val packageName: String,
    val className: String,
    val minWidth: Int,
    val minHeight: Int,
    val previewImage: Drawable? = null,
    val label: String = ""
)

/**
 * Shortcut model
 */
data class ShortcutInfo(
    val id: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val intent: android.content.Intent,
    val icon: Drawable? = null,
    val customIcon: Drawable? = null
)

/**
 * Folder model
 */
data class FolderInfo(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val items: MutableList<DesktopItem> = mutableListOf(),
    val isOpen: Boolean = false
)
