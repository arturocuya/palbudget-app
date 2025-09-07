package com.example.palbudget.repository

import android.util.Log
import com.example.palbudget.data.ImageInfo
import com.example.palbudget.database.dao.ImageDao
import com.example.palbudget.database.converters.toDomain
import com.example.palbudget.database.converters.toEntity
import com.example.palbudget.database.converters.withReceiptStatus
import com.example.palbudget.data.ReceiptAnalysis
import com.example.palbudget.viewmodel.ImageWithAnalysis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomImageRepository(
    private val dao: ImageDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val TAG = "RoomImageRepository"
    }

    val images: Flow<List<ImageWithAnalysis>> =
        dao.observeImagesWithAnalysis()
            .map { dbList -> 
                dbList.map { it.toDomain() }
            }
            .flowOn(ioDispatcher)

    val receipts: Flow<List<ImageWithAnalysis>> =
        dao.observeReceiptsWithAnalysis()
            .map { dbList -> 
                dbList.map { it.toDomain() }
            }
            .flowOn(ioDispatcher)

    suspend fun addImages(images: List<ImageInfo>) = withContext(ioDispatcher) {
        Log.d(TAG, "Adding ${images.size} images to database")
        dao.upsertImage(*images.toTypedArray())
    }

    suspend fun updateAnalysis(imageUri: String, analysis: ReceiptAnalysis) = withContext(ioDispatcher) {
        Log.d(TAG, "Updating analysis for image: $imageUri")
        
        // Don't update the ImageInfo record to preserve original dateCreated and avoid reordering
        // The receipt status can be determined from the presence of analysis data
        
        // Insert/update the analysis
        dao.upsertAnalysis(analysis.toEntity(imageUri))
        
        // Insert/update the items
        dao.upsertItems(analysis.items.map { it.toEntity(imageUri) })
    }

    suspend fun removeImage(imageWithAnalysis: ImageWithAnalysis) = withContext(ioDispatcher) {
        Log.d(TAG, "Removing image: ${imageWithAnalysis.imageInfo.uri}")
        dao.deleteImage(imageWithAnalysis.imageInfo.uri)
    }

    suspend fun removeAllImages() = withContext(ioDispatcher) {
        Log.d(TAG, "Removing all images")
        dao.deleteAll()
    }

    // Legacy methods for compatibility
    suspend fun loadImages(): List<ImageInfo> = withContext(ioDispatcher) {
        emptyList() // This will be replaced by the Flow-based approach
    }

    suspend fun saveImages(images: List<ImageInfo>) = addImages(images)
}
