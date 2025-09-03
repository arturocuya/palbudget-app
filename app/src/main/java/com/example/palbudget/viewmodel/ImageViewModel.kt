package com.example.palbudget.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.example.palbudget.data.ImageInfo

class ImageViewModel : ViewModel() {
    private val _images = mutableStateListOf<ImageInfo>()
    val images: SnapshotStateList<ImageInfo> = _images
    
    fun addImages(newImages: List<ImageInfo>) {
        _images.addAll(newImages)
    }
    
    fun loadImages(imageList: List<ImageInfo>) {
        _images.clear()
        _images.addAll(imageList)
    }
    
    fun removeImage(imageInfo: ImageInfo) {
        _images.remove(imageInfo)
    }
    
    fun getSortedImages(): List<ImageInfo> {
        return _images.sortedByDescending { it.dateCreated }
    }
    
    fun removeAllImages() {
        _images.clear()
    }
}
