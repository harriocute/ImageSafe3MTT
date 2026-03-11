package com.safetry.privacy.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.safetry.privacy.databinding.ActivityProcessingBinding
import com.safetry.privacy.model.PrivacyReport
import com.safetry.privacy.model.DetectionResult
import com.safetry.privacy.processor.ExifScrubber
import com.safetry.privacy.processor.AIRedactor
import com.safetry.privacy.processor.PrivacyScorer
import com.safetry.privacy.utils.PreferencesManager
import com.safetry.privacy.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProcessingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_SOURCE = "extra_source"
        const val SOURCE_SHARE = "share"
        const val SOURCE_MANUAL = "manual"
    }

    private lateinit var binding: ActivityProcessingBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var exifScrubber: ExifScrubber
    private lateinit var aiRedactor: AIRedactor
    private lateinit var privacyScorer: PrivacyScorer

    private var cleanedFilePath: String? = null
    private var privacyReport: PrivacyReport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        exifScrubber = ExifScrubber(this)
        aiRedactor = AIRedactor(this)
        privacyScorer = PrivacyScorer()

        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUriString == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)
        setupButtons()
        processImage(imageUri)
    }

    private fun setupButtons() {
        binding.btnShareClean.setOnClickListener {
            shareCleanFile()
        }
        binding.btnCancel.setOnClickListener {
            cleanupAndFinish()
        }
    }

    private fun processImage(imageUri: Uri) {
        showProcessingState()

        lifecycleScope.launch {
            try {
                val blurFaces = withContext(Dispatchers.IO) {
                    var blurFacesValue = false
                    preferencesManager.getBlurFaces().collect { blurFacesValue = it }
                    blurFacesValue
                }

                // Step 1: Load original bitmap
                val originalBitmap = withContext(Dispatchers.IO) {
                    loadBitmapFromUri(imageUri)
                }

                if (originalBitmap == null) {
                    showError("Could not load image")
                    return@launch
                }

                // Step 2: Analyze EXIF metadata
                binding.tvProcessingStatus.text = "Analyzing metadata..."
                val exifData = withContext(Dispatchers.IO) {
                    exifScrubber.analyzeExif(imageUri)
                }

                // Step 3: Run AI detection
                binding.tvProcessingStatus.text = "Running AI analysis..."
                val detections = withContext(Dispatchers.Default) {
                    aiRedactor.detectSensitiveContent(originalBitmap, blurFaces)
                }

                // Step 4: Apply redactions and blur
                binding.tvProcessingStatus.text = "Applying privacy protection..."
                val redactedBitmap = withContext(Dispatchers.Default) {
                    aiRedactor.applyRedactions(originalBitmap, detections)
                }

                // Step 5: Strip EXIF and save clean file
                binding.tvProcessingStatus.text = "Removing metadata..."
                val cleanFile = withContext(Dispatchers.IO) {
                    val tempFile = FileUtils.createTempImageFile(this@ProcessingActivity)
                    exifScrubber.saveCleanImage(redactedBitmap, tempFile)
                    tempFile
                }

                cleanedFilePath = cleanFile.absolutePath

                // Step 6: Generate privacy report
                val report = privacyScorer.generateReport(exifData, detections)
                privacyReport = report

                // Step 7: Show results
                withContext(Dispatchers.Main) {
                    showResults(redactedBitmap, detections, report)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Processing failed: ${e.message}")
                }
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun showProcessingState() {
        binding.layoutProcessing.visibility = View.VISIBLE
        binding.layoutResults.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.btnShareClean.isEnabled = false
    }

    private fun showResults(
        bitmap: Bitmap,
        detections: List<DetectionResult>,
        report: PrivacyReport
    ) {
        binding.layoutProcessing.visibility = View.GONE
        binding.layoutResults.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE

        // Show processed image with detection overlay
        binding.privacyImageView.setImageBitmap(bitmap)
        binding.privacyImageView.setDetections(detections)

        // Show privacy scores
        binding.tvScoreBefore.text = "Before: ${report.scoreBefore}/10"
        binding.tvScoreAfter.text = "After: ${report.scoreAfter}/10"
        binding.scoreProgressBefore.progress = report.scoreBefore * 10
        binding.scoreProgressAfter.progress = report.scoreAfter * 10

        // Show privacy report
        binding.tvPrivacyReport.text = buildReportText(report)

        // Enable share button
        binding.btnShareClean.isEnabled = true

        // Color score indicators
        updateScoreColor(report.scoreBefore, report.scoreAfter)
    }

    private fun buildReportText(report: PrivacyReport): String {
        val sb = StringBuilder()
        sb.appendLine("🛡️ PRIVACY ANALYSIS REPORT")
        sb.appendLine("─────────────────────────")

        if (report.risks.isEmpty()) {
            sb.appendLine("✅ No privacy risks detected!")
        } else {
            sb.appendLine("⚠️ Issues Found & Fixed:")
            report.risks.forEach { risk ->
                sb.appendLine("  • $risk")
            }
        }

        sb.appendLine()
        sb.appendLine("📊 Privacy Score")
        sb.appendLine("  Before: ${report.scoreBefore}/10 ${getScoreEmoji(report.scoreBefore)}")
        sb.appendLine("  After:  ${report.scoreAfter}/10 ${getScoreEmoji(report.scoreAfter)}")

        if (report.detectionSummary.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("🔍 AI Detections:")
            report.detectionSummary.forEach { detection ->
                sb.appendLine("  • $detection")
            }
        }

        sb.appendLine()
        sb.appendLine("✅ All processing done ON-DEVICE")
        sb.appendLine("   No data uploaded anywhere.")

        return sb.toString()
    }

    private fun getScoreEmoji(score: Int): String {
        return when {
            score >= 8 -> "🟢"
            score >= 5 -> "🟡"
            else -> "🔴"
        }
    }

    private fun updateScoreColor(before: Int, after: Int) {
        val beforeColor = when {
            before >= 8 -> getColor(android.R.color.holo_green_dark)
            before >= 5 -> getColor(android.R.color.holo_orange_dark)
            else -> getColor(android.R.color.holo_red_dark)
        }
        val afterColor = when {
            after >= 8 -> getColor(android.R.color.holo_green_dark)
            after >= 5 -> getColor(android.R.color.holo_orange_dark)
            else -> getColor(android.R.color.holo_red_dark)
        }
        binding.tvScoreBefore.setTextColor(beforeColor)
        binding.tvScoreAfter.setTextColor(afterColor)
    }

    private fun showError(message: String) {
        binding.layoutProcessing.visibility = View.GONE
        binding.layoutResults.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
    }

    private fun shareCleanFile() {
        val filePath = cleanedFilePath ?: run {
            Toast.makeText(this, "Clean file not ready", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(this, "Clean file not found", Toast.LENGTH_SHORT).show()
            return
        }

        val fileUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Clean Image via..."))
    }

    private fun cleanupAndFinish() {
        cleanedFilePath?.let { path ->
            lifecycleScope.launch(Dispatchers.IO) {
                File(path).delete()
            }
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        aiRedactor.close()
    }
}
