package com.example.palbudget

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.palbudget.service.ReceiptAnalysis
import kotlinx.coroutines.launch
import com.example.palbudget.repository.ImageRepository
import com.example.palbudget.service.ImageAnalysis
import com.example.palbudget.service.OpenAIService
import com.example.palbudget.ui.theme.PalBudgetTheme
import com.example.palbudget.utils.ImageUtils
import com.example.palbudget.viewmodel.ImageViewModel
import androidx.core.net.toUri
import com.example.palbudget.views.MainScreen

class MainActivity : ComponentActivity() {
    private val viewModel: ImageViewModel by viewModels()
    private lateinit var imageRepository: ImageRepository

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickSingleImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickMultipleImagesLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    private var tempCameraUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageRepository = ImageRepository(this)
        setupImageLaunchers()

        enableEdgeToEdge()
        setContent {
            PalBudgetTheme {
                MainScreen(
                    images = viewModel.images,
                    onLoadImages = { viewModel.loadImages(it) },
                    onTakePhoto = ::launchCamera,
                    onPickMultiple = ::launchMultipleImagePicker,
                    onRemoveAll = ::removeAllImages,
                    onRemoveSelected = { imageWithAnalysisList ->
                        imageWithAnalysisList.forEach { viewModel.removeImage(it) }
                    },
                    onAnalyzeSelected = ::analyzeSelectedImages
                )
            }
        }
    }

    private fun setupImageLaunchers() {
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && tempCameraUri != null) {
                val imageInfo = ImageUtils.uriToImageInfo(this, tempCameraUri!!)
                viewModel.addImages(listOf(imageInfo))
                Toast.makeText(
                    this,
                    getString(R.string.photo_captured_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.w("PalBudget", "Camera capture failed - success: $success, uri: $tempCameraUri")
            }
            tempCameraUri = null
        }

        pickSingleImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let {
                val imageInfo = ImageUtils.uriToImageInfo(this, it)
                viewModel.addImages(listOf(imageInfo))
            }
        }

        pickMultipleImagesLauncher = registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
        ) { uris ->
            if (uris.isNotEmpty()) {
                // Take persistable URI permissions
                uris.forEach { uri ->
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        Log.d("PalBudget", "Could not take persistable permission for $uri", e)
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.some_images_may_not_persist),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                val imageInfos = uris.map { ImageUtils.uriToImageInfo(this, it) }
                viewModel.addImages(imageInfos)
            }
        }
    }

    fun launchCamera() {
        val uri = ImageUtils.createImageUri(this)
        uri?.let {
            tempCameraUri = it
            takePictureLauncher.launch(it)
        } ?: run {
            Log.e("PalBudget", "Failed to create camera URI")
            Toast.makeText(this, getString(R.string.failed_to_create_camera_uri), Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun launchMultipleImagePicker() {
        pickMultipleImagesLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun removeAllImages() {
        viewModel.removeAllImages()
        Toast.makeText(this, getString(R.string.all_images_removed), Toast.LENGTH_SHORT).show()
    }

    private fun analyzeSelectedImages(selectedImagesUris: Set<String>) {
        val selectedImages = viewModel.images.filter { selectedImagesUris.contains(it.imageInfo.uri) }

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "No images selected for analysis", Toast.LENGTH_LONG).show()
            return
        }

        // Get original URIs
        val originalUris = selectedImages.map { it.imageInfo.uri }

        // Convert image URIs to base64 format
        val imageBase64List = selectedImages.mapNotNull { imageWithAnalysis ->
            ImageUtils.uriToBase64(this, imageWithAnalysis.imageInfo.uri.toUri())
        }

        // Launch coroutine to perform analysis
        val openAIService = OpenAIService(this)

        if (imageBase64List.isEmpty()) {
            Toast.makeText(this, "Failed to convert images to base64", Toast.LENGTH_LONG).show()
            return
        }

        // Show loading toast
        Toast.makeText(this, "Analyzing ${imageBase64List.size} image(s)...", Toast.LENGTH_SHORT)
            .show()

        // Use lifecycleScope for proper coroutine scope tied to activity lifecycle
        lifecycleScope.launch {
            val result = openAIService.analyzeReceipts(imageBase64List, originalUris)

            // Show result (already on main thread with lifecycleScope)
            if (result.success) {
                // Update viewModel with analysis results
                result.results.forEach { imageAnalysis ->
                    val originalUri = if (imageAnalysis.imageIndex < originalUris.size) {
                        originalUris[imageAnalysis.imageIndex]
                    } else null
                    
                    if (originalUri != null) {
                        // Find the ImageInfo in the ViewModel by URI
                        val imageInfo = viewModel.images.find { it.imageInfo.uri == originalUri }?.imageInfo
                        if (imageInfo != null) {
                            Log.d("MainActivity", "Updating analysis for image: ${imageInfo.uri}, isReceipt: ${imageAnalysis.isReceipt}")
                            // For non-receipts, create an empty ReceiptAnalysis to indicate it was analyzed
                            val analysisToStore = imageAnalysis.analysis ?: ReceiptAnalysis(
                                items = emptyList(),
                                category = "",
                                finalPrice = 0,
                                date = null
                            )
                            viewModel.updateAnalysis(imageInfo, analysisToStore)
                        }
                    }
                }
                
                val summary = buildAnalysisSummary(result.results)
                Toast.makeText(this@MainActivity, summary, Toast.LENGTH_LONG).show()
            } else {
                val errorMessage = result.error ?: "Unknown error occurred"
                Toast.makeText(
                    this@MainActivity,
                    "Analysis failed: $errorMessage",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun buildAnalysisSummary(results: List<ImageAnalysis>): String {
        val receipts = results.filter { it.isReceipt }
        val nonReceipts = results.filter { !it.isReceipt }

        val summary = StringBuilder()

        if (receipts.isEmpty() && nonReceipts.isNotEmpty()) {
            summary.append("No receipts found in ${nonReceipts.size} image(s)")
        } else if (receipts.isNotEmpty()) {
            summary.append("Found ${receipts.size} receipt(s):\n\n")

            receipts.forEach { result ->
                result.analysis?.let { analysis ->
                    summary.append("• ${analysis.category.uppercase()}: $${analysis.finalPrice / 100.0}\n")
                    summary.append("  Date: ${analysis.date ?: "Not available"}\n")
                    summary.append("  ${analysis.items.size} item(s)\n")
                }
            }

            if (nonReceipts.isNotEmpty()) {
                summary.append("\n${nonReceipts.size} image(s) were not receipts")
            }
        }

        return summary.toString()
    }
}

