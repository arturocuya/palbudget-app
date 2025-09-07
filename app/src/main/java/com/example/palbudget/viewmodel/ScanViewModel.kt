package com.example.palbudget.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.example.palbudget.data.ImageInfo
import com.example.palbudget.data.ReceiptAnalysis
import com.example.palbudget.service.ImageAnalysis
import com.example.palbudget.service.OpenAIService
import com.example.palbudget.utils.ImageUtils
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : BaseImageViewModel(application) {

    companion object {
        private const val TAG = "ScanViewModel"
    }

    private val _images = mutableStateListOf<ImageWithAnalysis>()
    val images: SnapshotStateList<ImageWithAnalysis> = _images
    
    private val _isAnalyzing = mutableStateOf(false)
    val isAnalyzing: Boolean get() = _isAnalyzing.value

    fun addImages(newImages: List<ImageInfo>) {
        Log.d(TAG, "Adding ${newImages.size} images to scan list")
        // Add new images at the beginning, sorted by dateCreated (most recent first)
        val newImageWithAnalysis = newImages.map { ImageWithAnalysis(it, null) }
        _images.addAll(0, newImageWithAnalysis)
    }

    fun removeImages(imagesToRemove: List<ImageWithAnalysis>) {
        Log.d(TAG, "Removing ${imagesToRemove.size} images from scan list")
        _images.removeAll(imagesToRemove)
    }

    fun removeAllImages() {
        Log.d(TAG, "Removing all images from scan list")
        _images.clear()
    }

    fun analyzeSelected(
        context: Context,
        selectedImagesUris: Set<String>,
        onToast: (String) -> Unit = { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    ) {
        val selectedImages = _images.filter { selectedImagesUris.contains(it.imageInfo.uri) }

        if (selectedImages.isEmpty()) {
            onToast("No images selected for analysis")
            return
        }

        Log.d(TAG, "Analyzing ${selectedImages.size} images")

        // Get original URIs
        val originalUris = selectedImages.map { it.imageInfo.uri }

        // Convert image URIs to base64 format
        val imageBase64List = selectedImages.mapNotNull { imageWithAnalysis ->
            ImageUtils.uriToBase64(context, imageWithAnalysis.imageInfo.uri.toUri())
        }

        if (imageBase64List.isEmpty()) {
            onToast("Failed to convert images to base64")
            return
        }

        // Show loading toast
        onToast("Analyzing ${imageBase64List.size} image(s)...")

        // Launch analysis coroutine
        viewModelScope.launch {
            _isAnalyzing.value = true
            val openAIService = OpenAIService(context)
            val result = openAIService.analyzeReceipts(imageBase64List, originalUris)

            // Process analysis results
            if (result.success) {
                // Process each analysis result
                result.results.forEach { imageAnalysis ->
                    val originalUri = if (imageAnalysis.imageIndex < originalUris.size) {
                        originalUris[imageAnalysis.imageIndex]
                    } else null

                    if (originalUri != null) {
                        val imageIndex = _images.indexOfFirst { it.imageInfo.uri == originalUri }
                        if (imageIndex != -1) {
                            val currentImage = _images[imageIndex]

                            if (imageAnalysis.isReceipt && imageAnalysis.analysis != null) {
                                Log.d(TAG, "Receipt detected, saving to database and showing ✅: $originalUri")
                                // Save to database
                                repository.addImages(listOf(currentImage.imageInfo))
                                repository.updateAnalysis(originalUri, imageAnalysis.analysis)
                                // Update scan list with actual analysis to show ✅
                                _images[imageIndex] = ImageWithAnalysis(
                                    currentImage.imageInfo,
                                    imageAnalysis.analysis
                                )
                            } else if (!imageAnalysis.isReceipt) {
                                Log.d(TAG, "Non-receipt detected, showing ❌: $originalUri")
                                // Update scan list with placeholder analysis to show ❌
                                val placeholderAnalysis = ReceiptAnalysis(
                                    items = emptyList(),
                                    category = "",
                                    finalPrice = 0,
                                    date = null
                                )
                                _images[imageIndex] = ImageWithAnalysis(
                                    currentImage.imageInfo,
                                    placeholderAnalysis
                                )
                            }
                        }
                    }
                }

                val summary = buildAnalysisSummary(result.results)
                onToast(summary)
            } else {
                val errorMessage = result.error ?: "Unknown error occurred"
                onToast("Analysis failed: $errorMessage")
            }
            _isAnalyzing.value = false
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
