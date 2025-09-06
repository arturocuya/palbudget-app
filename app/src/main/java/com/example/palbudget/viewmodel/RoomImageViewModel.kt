package com.example.palbudget.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.palbudget.data.ImageInfo
import com.example.palbudget.database.AppDatabase
import com.example.palbudget.repository.RoomImageRepository
import com.example.palbudget.data.ReceiptAnalysis
import kotlinx.coroutines.launch

class RoomImageViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "RoomImageViewModel"
    }

    private val repository: RoomImageRepository
    private val _images = mutableStateListOf<ImageWithAnalysis>()
    val images: SnapshotStateList<ImageWithAnalysis> = _images

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RoomImageRepository(database.imageDao())
        
        // Observe the database and update the state list
        viewModelScope.launch {
            repository.images.collect { imageList ->
                Log.d(TAG, "Received ${imageList.size} images from repository")
                
                // Instead of clearing and re-adding all items, update only changed items to preserve position
                val currentImageUris = _images.map { it.imageInfo.uri }.toSet()
                val newImageUris = imageList.map { it.imageInfo.uri }.toSet()
                
                // Remove items that are no longer in the database
                _images.removeAll { !newImageUris.contains(it.imageInfo.uri) }
                
                // Update existing items and add new ones
                imageList.forEach { newImage ->
                    val existingIndex = _images.indexOfFirst { it.imageInfo.uri == newImage.imageInfo.uri }
                    if (existingIndex != -1) {
                        // Update existing item in place
                        _images[existingIndex] = newImage
                    } else {
                        // Add new item (find correct position based on dateCreated to maintain sort order)
                        val insertIndex = _images.indexOfFirst { it.imageInfo.dateCreated < newImage.imageInfo.dateCreated }
                        if (insertIndex != -1) {
                            _images.add(insertIndex, newImage)
                        } else {
                            _images.add(newImage)
                        }
                    }
                }
                
                Log.d(TAG, "Updated image list, now has ${_images.size} items")
            }
        }
    }

    fun addImages(newImages: List<ImageInfo>) {
        viewModelScope.launch {
            try {
                repository.addImages(newImages)
                Log.d(TAG, "Added ${newImages.size} images")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding images", e)
            }
        }
    }

    fun loadImages(imageList: List<ImageInfo>) {
        // This is now handled automatically by the Flow from repository
        // But we can still support this method for compatibility
        viewModelScope.launch {
            try {
                repository.addImages(imageList)
                Log.d(TAG, "Loaded ${imageList.size} images")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading images", e)
            }
        }
    }

    fun removeImage(imageWithAnalysis: ImageWithAnalysis) {
        viewModelScope.launch {
            try {
                repository.removeImage(imageWithAnalysis)
                Log.d(TAG, "Removed image: ${imageWithAnalysis.imageInfo.uri}")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing image", e)
            }
        }
    }

    fun removeAllImages() {
        viewModelScope.launch {
            try {
                repository.removeAllImages()
                Log.d(TAG, "Removed all images")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing all images", e)
            }
        }
    }

    fun updateAnalysis(imageInfo: ImageInfo, analysis: ReceiptAnalysis) {
        viewModelScope.launch {
            try {
                repository.updateAnalysis(imageInfo.uri, analysis)
                Log.d(TAG, "Updated analysis for image: ${imageInfo.uri}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating analysis", e)
            }
        }
    }
}
