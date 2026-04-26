package com.jiliu.launcher.util

import android.content.Context
import android.net.Uri
import java.io.File

object FileProviderUtil {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /**
     * Get FileProvider URI for file
     */
    fun getUriForFile(context: Context, file: File): Uri {
        val authority = context.packageName + AUTHORITY_SUFFIX
        return androidx.core.content.FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Get FileProvider URI for path
     */
    fun getUriForPath(context: Context, path: String): Uri {
        return getUriForFile(context, File(path))
    }
}
