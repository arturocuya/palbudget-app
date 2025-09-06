package com.example.palbudget.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.palbudget.data.ImageInfo
import com.example.palbudget.data.ReceiptAnalysisEntity
import com.example.palbudget.data.ReceiptItemEntity
import com.example.palbudget.database.relations.ImageWithAnalysisDb
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {

    /* INSERT / UPDATE */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImage(vararg image: ImageInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnalysis(analysis: ReceiptAnalysisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<ReceiptItemEntity>)

    /* READ */
    @Transaction
    @Query("SELECT * FROM images ORDER BY dateCreated DESC")
    fun observeImagesWithAnalysis(): Flow<List<ImageWithAnalysisDb>>

    @Transaction
    @Query("SELECT * FROM images WHERE uri = :uri")
    suspend fun getImage(uri: String): ImageWithAnalysisDb?

    /* DELETE */
    @Query("DELETE FROM images WHERE uri = :uri")
    suspend fun deleteImage(uri: String)

    @Query("DELETE FROM images")
    suspend fun deleteAll()
}
