package com.example.palbudget.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.palbudget.database.dao.ImageDao
import com.example.palbudget.data.ImageInfo
import com.example.palbudget.data.ReceiptAnalysisEntity
import com.example.palbudget.data.ReceiptItemEntity

@Database(
    version = 1,
    entities = [
        ImageInfo::class,
        ReceiptAnalysisEntity::class,
        ReceiptItemEntity::class
    ],
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "palbudget.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
