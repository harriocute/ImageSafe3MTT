package com.safetry.privacy.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * This activity intercepts Android share intents.
 * It appears in the Android share menu as "Privacy Ghost".
 * When selected, it immediately launches the ProcessingActivity.
 */
class ShareReceiverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                handleSingleImage(intent)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                handleMultipleImages(intent)
            }
            else -> {
                finish()
            }
        }
    }

    private fun handleSingleImage(intent: Intent) {
        val imageUri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        if (imageUri != null) {
            launchProcessing(imageUri)
        } else {
            finish()
        }
    }

    private fun handleMultipleImages(intent: Intent) {
        val imageUris: ArrayList<Uri>? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }

        if (!imageUris.isNullOrEmpty()) {
            // For multiple images, process the first one
            // Future enhancement: batch process all images
            launchProcessing(imageUris[0])
        } else {
            finish()
        }
    }

    private fun launchProcessing(uri: Uri) {
        val processingIntent = Intent(this, ProcessingActivity::class.java).apply {
            putExtra(ProcessingActivity.EXTRA_IMAGE_URI, uri.toString())
            putExtra(ProcessingActivity.EXTRA_SOURCE, ProcessingActivity.SOURCE_SHARE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(processingIntent)
        finish()
    }
}
