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
import com.example.palbudget.data.ReceiptCategory
import com.example.palbudget.service.ImageAnalysis
import com.example.palbudget.service.OpenAIService
import com.example.palbudget.utils.ImageUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class ScanViewModel(application: Application) : BaseImageViewModel(application) {

    companion object {
        private const val TAG = "ScanViewModel"
        private const val MAX_CONCURRENT_ANALYSIS = 3
    }

    private val _images = mutableStateListOf<ImageWithAnalysis>()
    val images: SnapshotStateList<ImageWithAnalysis> = _images
    
    private val _isAnalyzing = mutableStateOf(false)
    val isAnalyzing: Boolean get() = _isAnalyzing.value
    
    private val imagesMutex = Mutex()

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

        Log.d(TAG, "Analyzing ${selectedImages.size} images in parallel")
        onToast("Analyzing ${selectedImages.size} image(s)...")

        viewModelScope.launch {
            _isAnalyzing.value = true
            
            val results = mutableListOf<ImageAnalysis>()
            val openAIService = OpenAIService(context)
            
            // Process images in parallel with concurrency limit
            selectedImages.chunked(MAX_CONCURRENT_ANALYSIS).forEachIndexed { chunkIndex, chunk ->
                val deferreds = chunk.mapIndexed { imageIndex, imageWithAnalysis ->
                    async(Dispatchers.IO) {
                        val globalIndex = chunkIndex * MAX_CONCURRENT_ANALYSIS + imageIndex
                        analyzeSingleImage(context, imageWithAnalysis, openAIService, globalIndex)
                    }
                }
                
                deferreds.forEachIndexed { deferredIndex, deferred ->
                    val result = deferred.await()
                    if (result != null) {
                        results.add(result)
                        val globalImageIndex = chunkIndex * MAX_CONCURRENT_ANALYSIS + deferredIndex
                        updateImageState(result, globalImageIndex, selectedImages)
                    }
                }
            }

            _isAnalyzing.value = false
        }
    }

    private suspend fun analyzeSingleImage(
        context: Context,
        imageWithAnalysis: ImageWithAnalysis,
        openAIService: OpenAIService,
        imageIndex: Int
    ): ImageAnalysis? {
        val imageBase64 = ImageUtils.uriToBase64(context, imageWithAnalysis.imageInfo.uri.toUri())
            ?: return null
        
        val result = openAIService.analyzeReceipts(
            listOf(imageBase64), 
            listOf(imageWithAnalysis.imageInfo.uri)
        )
        
        return if (result.success && result.results.isNotEmpty()) {
            result.results.first()
        } else {
            if (!result.success) {
                // Show error to user via toast on main thread
                result.error?.let { errorMessage ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Analysis failed: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            }
            Log.d(TAG, "Failed to analyze image: ${imageWithAnalysis.imageInfo.uri}")
            null
        }
    }
    
    private suspend fun updateImageState(imageAnalysis: ImageAnalysis, originalImageIndex: Int, selectedImages: List<ImageWithAnalysis>) {
        imagesMutex.withLock {
            if (originalImageIndex < selectedImages.size) {
                val targetImage = selectedImages[originalImageIndex]
                val imageIndex = _images.indexOfFirst { it === targetImage }
                
                if (imageIndex != -1) {
                    val currentImage = _images[imageIndex]
                
                    if (imageAnalysis.isReceipt && imageAnalysis.analysis != null) {
                        Log.d(TAG, "Receipt detected, saving to database and showing ✅: ${currentImage.imageInfo.uri}")
                        // Save to database - use the original URI, not the transformed one
                        repository.addImages(listOf(currentImage.imageInfo))
                        repository.updateAnalysis(currentImage.imageInfo.uri, imageAnalysis.analysis)
                        // Update scan list with actual analysis to show ✅
                        _images[imageIndex] = ImageWithAnalysis(
                            currentImage.imageInfo,
                            imageAnalysis.analysis
                        )
                    } else if (!imageAnalysis.isReceipt) {
                        Log.d(TAG, "Non-receipt detected, showing ❌: ${currentImage.imageInfo.uri}")
                        // Update scan list with placeholder analysis to show ❌
                        val placeholderAnalysis = ReceiptAnalysis(
                            items = emptyList(),
                            category = ReceiptCategory("Not a receipt", 0),
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
                    summary.append("• ${analysis.category.name.uppercase()}: $${analysis.finalPrice / 100.0}\n")
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
