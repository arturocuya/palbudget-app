package com.example.palbudget.database

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.palbudget.repository.ImageRepository as OldImageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SeedFromFileRepositoryCallback(private val appContext: Context) : RoomDatabase.Callback() {

    companion object {
        private const val TAG = "SeedFromFileRepository"
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        Log.d(TAG, "Database created, starting migration from file repository")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load from old file repository
                val oldRepository = OldImageRepository(appContext)
                val images = oldRepository.loadImages()

                if (images.isNotEmpty()) {
                    Log.d(TAG, "Migrating ${images.size} images from file repository to Room")

                    // Get the Room database instance
                    val roomDb = AppDatabase.getDatabase(appContext)
                    val dao = roomDb.imageDao()

                    // Insert all images
                    dao.upsertImage(*images.toTypedArray())

                    Log.d(TAG, "Migration completed successfully")
                } else {
                    Log.d(TAG, "No images found in old repository, skipping migration")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during migration from file repository", e)
            }
        }
    }
}
