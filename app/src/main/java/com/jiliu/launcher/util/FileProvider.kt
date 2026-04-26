package com.jiliu.launcher.util

import android.content.Context
import android.content.FileProvider
import java.io.File

object FileProvider {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /**
     * Get FileProvider URI for file
     */
    fun getUriForFile(context: Context, file: File): android.net.Uri {
        val authority = context.packageName + AUTHORITY_SUFFIX
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Get FileProvider URI for path
     */
    fun getUriForPath(context: Context, path: String): android.net.Uri {
        return getUriForFile(context, File(path))
    }
}
