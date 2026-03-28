package com.safetry.privacy.ui

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.safetry.privacy.databinding.ActivityProcessingBinding
import com.safetry.privacy.model.DetectionResult
import com.safetry.privacy.model.PrivacyReport
import com.safetry.privacy.processor.AIRedactor
import com.safetry.privacy.processor.ExifScrubber
import com.safetry.privacy.processor.PrivacyScorer
import com.safetry.privacy.utils.FileUtils
import com.safetry.privacy.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProcessingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_SOURCE = "extra_source"
        const val SOURCE_SHARE = "share"
        const val SOURCE_MANUAL = "manual"
    }

    private lateinit var binding: ActivityProcessingBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var exifScrubber: ExifScrubber
    private lateinit var aiRedactor: AIRedactor
    private lateinit var privacyScorer: PrivacyScorer
    private var cleanedFilePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        exifScrubber = ExifScrubber(this)
        aiRedactor = AIRedactor(this)
        privacyScorer = PrivacyScorer()

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupButtons()
        processImage(Uri.parse(imageUriString))
    }

    private fun setupButtons() {
        binding.btnShareClean.setOnClickListener { shareCleanFile() }
        binding.btnSaveToDevice.setOnClickListener { saveToDevice() }
        binding.btnCancel.setOnClickListener { cleanupAndFinish() }
    }

    private fun processImage(imageUri: Uri) {
        showProcessingState()

        lifecycleScope.launch {
            try {
                updateStatus("Loading image...")
                val originalBitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromUri(imageUri)
                } ?: run {
                    showError("Could not load image. Try a different photo.")
                    return@launch
                }

                val blurFaces = withContext(Dispatchers.IO) { prefs.getBlurFaces().first() }
                val blurPlates = withContext(Dispatchers.IO) { prefs.getBlurLicensePlates().first() }
                val blurSigns = withContext(Dispatchers.IO) { prefs.getBlurStreetSigns().first() }
                val blurBadges = withContext(Dispatchers.IO) { prefs.getBlurIdBadges().first() }
                val blurDocs = withContext(Dispatchers.IO) { prefs.getBlurTextDocs().first() }

                updateStatus("Analyzing metadata...")
                val exifData = withContext(Dispatchers.IO) {
                    exifScrubber.analyzeExif(imageUri)
                }

                updateStatus("Running AI analysis...")
                val detections = withContext(Dispatchers.Default) {
                    try {
                        aiRedactor.detectSensitiveContent(
                            bitmap = originalBitmap,
                            blurFaces = blurFaces,
                            blurLicensePlates = blurPlates,
                            blurStreetSigns = blurSigns,
                            blurIdBadges = blurBadges,
                            blurTextDocs = blurDocs
                        )
                    } catch (e: Exception) { emptyList() }
                }

                updateStatus("Applying privacy protection...")
                val redactedBitmap = withContext(Dispatchers.Default) {
                    try { aiRedactor.applyRedactions(originalBitmap, detections) }
                    catch (e: Exception) { originalBitmap }
                }

                updateStatus("Saving clean image...")
                val cleanFile = withContext(Dispatchers.IO) {
                    val tempFile = FileUtils.createTempImageFile(this@ProcessingActivity)
                    exifScrubber.saveCleanImage(redactedBitmap, tempFile)
                    tempFile
                }
                cleanedFilePath = cleanFile.absolutePath

                val report = privacyScorer.generateReport(exifData, detections)
                showResults(redactedBitmap, detections, report)

            } catch (e: Exception) {
                showError("Processing failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun updateStatus(message: String) {
        binding.tvProcessingStatus.text = message
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            val maxSize = 2048
            var scale = 1
            while (options.outWidth / scale > maxSize || options.outHeight / scale > maxSize) scale *= 2
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        } catch (e: Exception) { null }
    }

    private fun showProcessingState() {
        binding.layoutProcessing.visibility = View.VISIBLE
        binding.layoutResults.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.btnShareClean.isEnabled = false
        binding.btnSaveToDevice.isEnabled = false
    }

    private fun showResults(
        bitmap: Bitmap,
        detections: List<DetectionResult>,
        report: PrivacyReport
    ) {
        binding.layoutProcessing.visibility = View.GONE
        binding.layoutResults.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE

        binding.privacyImageView.setImageBitmap(bitmap)
        binding.privacyImageView.setDetections(detections)

        binding.tvScoreBefore.text = "Before: ${report.scoreBefore}/10"
        binding.tvScoreAfter.text = "After:  ${report.scoreAfter}/10"
        binding.scoreProgressBefore.progress = report.scoreBefore * 10
        binding.scoreProgressAfter.progress = report.scoreAfter * 10
        binding.tvPrivacyReport.text = buildReportText(report)
        binding.btnShareClean.isEnabled = true
        binding.btnSaveToDevice.isEnabled = true

        binding.tvScoreBefore.setTextColor(when {
            report.scoreBefore >= 8 -> getColor(android.R.color.holo_green_dark)
            report.scoreBefore >= 5 -> getColor(android.R.color.holo_orange_dark)
            else -> getColor(android.R.color.holo_red_dark)
        })
        binding.tvScoreAfter.setTextColor(getColor(android.R.color.holo_green_dark))
    }

    private fun buildReportText(report: PrivacyReport): String {
        val sb = StringBuilder()
        sb.appendLine("🛡️ PRIVACY ANALYSIS REPORT")
        sb.appendLine("─────────────────────────")
        if (report.risks.isEmpty()) sb.appendLine("✅ No privacy risks detected!")
        else { sb.appendLine("⚠️ Issues Found & Fixed:"); report.risks.forEach { sb.appendLine("  • $it") } }
        sb.appendLine()
        sb.appendLine("📊 Privacy Score")
        sb.appendLine("  Before: ${report.scoreBefore}/10")
        sb.appendLine("  After:  ${report.scoreAfter}/10")
        if (report.detectionSummary.isNotEmpty()) {
            sb.appendLine(); sb.appendLine("🔍 AI Detections:")
            report.detectionSummary.forEach { sb.appendLine("  • $it") }
        }
        sb.appendLine(); sb.appendLine("✅ 100% On-Device. No data uploaded.")
        return sb.toString()
    }

    private fun saveToDevice() {
        val filePath = cleanedFilePath ?: run {
            Toast.makeText(this, "No clean file ready", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) {
                    saveImageToGallery(filePath)
                }
                if (saved) {
                    Toast.makeText(
                        this@ProcessingActivity,
                        "✅ Saved to Gallery → ImageSafe",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnSaveToDevice.text = "✅ Saved!"
                    binding.btnSaveToDevice.isEnabled = false
                } else {
                    Toast.makeText(
                        this@ProcessingActivity,
                        "Failed to save image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProcessingActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveImageToGallery(filePath: String): Boolean {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "ImageSafe_$timestamp.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/ImageSafe")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return false

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(filePath).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
            true
        } else {
            // Android 9 and below
            val picturesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val imageSafeDir = File(picturesDir, "ImageSafe")
            if (!imageSafeDir.exists()) imageSafeDir.mkdirs()

            val destFile = File(imageSafeDir, fileName)
            File(filePath).copyTo(destFile, overwrite = true)

            // Notify gallery
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(destFile)
            sendBroadcast(mediaScanIntent)
            true
        }
    }

    private fun shareCleanFile() {
        val file = cleanedFilePath?.let { File(it) } ?: return
        if (!file.exists()) {
            Toast.makeText(this, "Clean file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val fileUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share Clean Image via..."))
    }

    private fun cleanupAndFinish() {
        cleanedFilePath?.let {
            lifecycleScope.launch(Dispatchers.IO) { File(it).delete() }
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        aiRedactor.close()
    }
}
