package com.example.palbudget.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.example.palbudget.data.ImageInfo
import com.example.palbudget.data.ReceiptAnalysis

data class ImageWithAnalysis(
    val imageInfo: ImageInfo,
    val analysis: ReceiptAnalysis? = null
)

class ImageViewModel : ViewModel() {
    private val _images = mutableStateListOf<ImageWithAnalysis>()
    val images: SnapshotStateList<ImageWithAnalysis> = _images
    
    fun addImages(newImages: List<ImageInfo>) {
        val wrappedImages = newImages.map { ImageWithAnalysis(it) }
        _images.addAll(wrappedImages)
    }
    
    fun loadImages(imageList: List<ImageInfo>) {
        _images.clear()
        val wrappedImages = imageList.map { ImageWithAnalysis(it) }
        _images.addAll(wrappedImages)
    }
    
    fun removeImage(imageWithAnalysis: ImageWithAnalysis) {
        _images.remove(imageWithAnalysis)
    }

    fun removeAllImages() {
        _images.clear()
    }
    
    fun updateAnalysis(imageInfo: ImageInfo, analysis: ReceiptAnalysis) {
        val index = _images.indexOfFirst { it.imageInfo == imageInfo }
        if (index != -1) {
            _images[index] = _images[index].copy(analysis = analysis)
        }
    }
}
