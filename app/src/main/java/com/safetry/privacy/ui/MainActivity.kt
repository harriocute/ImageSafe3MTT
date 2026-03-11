package com.safetry.privacy.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.safetry.privacy.databinding.ActivityMainBinding
import com.safetry.privacy.utils.PreferencesManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesManager: PreferencesManager

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { launchProcessing(it) }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(this, "Storage permission required to scan files", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        setupUI()
        loadPreferences()
    }

    private fun setupUI() {
        // Scan a File button
        binding.btnScanFile.setOnClickListener {
            checkPermissionsAndPickImage()
        }

        // Enable Share Protection button
        binding.btnShareProtection.setOnClickListener {
            showShareProtectionInfo()
        }

        // Blur Faces toggle
        binding.switchBlurFaces.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.setBlurFaces(isChecked)
            }
        }

        // Auto Remove Metadata toggle
        binding.switchAutoRemoveMetadata.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                preferencesManager.setAutoRemoveMetadata(isChecked)
            }
        }

        // Privacy history button
        binding.btnPrivacyHistory.setOnClickListener {
            // Future: show history of processed files
            Toast.makeText(this, "Privacy history coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPreferences() {
        lifecycleScope.launch {
            preferencesManager.getBlurFaces().collect { blurFaces ->
                binding.switchBlurFaces.isChecked = blurFaces
            }
        }
        lifecycleScope.launch {
            preferencesManager.getAutoRemoveMetadata().collect { autoRemove ->
                binding.switchAutoRemoveMetadata.isChecked = autoRemove
            }
        }
    }

    private fun checkPermissionsAndPickImage() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            pickImageLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun launchProcessing(uri: Uri) {
        val intent = Intent(this, ProcessingActivity::class.java).apply {
            putExtra(ProcessingActivity.EXTRA_IMAGE_URI, uri.toString())
            putExtra(ProcessingActivity.EXTRA_SOURCE, ProcessingActivity.SOURCE_MANUAL)
        }
        startActivity(intent)
    }

    private fun showShareProtectionInfo() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Share Protection Active")
            .setMessage(
                "Privacy Ghost is ready to protect your photos!\n\n" +
                "HOW TO USE:\n" +
                "1. Open your Gallery or Files app\n" +
                "2. Select any photo\n" +
                "3. Tap the Share button\n" +
                "4. Choose 'Privacy Ghost' from the share menu\n" +
                "5. Review the privacy report\n" +
                "6. Tap 'Share Clean File' to share safely\n\n" +
                "Privacy Ghost will automatically:\n" +
                "• Remove GPS and metadata\n" +
                "• Detect and blur license plates\n" +
                "• Detect and blur street signs\n" +
                "• Detect and blur ID badges\n" +
                (if (binding.switchBlurFaces.isChecked) "• Blur detected faces\n" else "") +
                "\nAll processing happens ON YOUR DEVICE. No data is uploaded."
            )
            .setPositiveButton("Got it!") { dialog, _ -> dialog.dismiss() }
            .create()
        dialog.show()
    }
}
