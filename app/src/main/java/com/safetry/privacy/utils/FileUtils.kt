package com.safetry.privacy.utils

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    private const val TEMP_DIR = "privacy_temp"

    /**
     * Creates a temporary file for storing the cleaned image.
     * These files should be deleted after sharing.
     */
    fun createTempImageFile(context: Context): File {
        val tempDir = File(context.cacheDir, TEMP_DIR)
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(tempDir, "clean_${timestamp}.jpg")
    }

    /**
     * Cleans up all temporary files older than 1 hour.
     */
    fun cleanupOldTempFiles(context: Context) {
        val tempDir = File(context.cacheDir, TEMP_DIR)
        if (!tempDir.exists()) return

        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        tempDir.listFiles()?.forEach { file ->
            if (file.lastModified() < oneHourAgo) {
                file.delete()
            }
        }
    }

    /**
     * Gets the size of a file in human-readable format.
     */
    fun getReadableFileSize(sizeBytes: Long): String {
        return when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
        }
    }
}
