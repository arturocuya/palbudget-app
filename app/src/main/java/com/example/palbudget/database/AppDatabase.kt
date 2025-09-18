package com.example.palbudget.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.palbudget.database.dao.ImageDao
import com.example.palbudget.data.CategoryDefaults
import com.example.palbudget.data.ImageInfo
import com.example.palbudget.data.ReceiptAnalysisEntity
import com.example.palbudget.data.ReceiptItemEntity

@Database(
    version = 2,
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
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new category columns with NOT NULL and default values
                db.execSQL("ALTER TABLE receipt_analysis ADD COLUMN category_name TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE receipt_analysis ADD COLUMN category_color INTEGER NOT NULL DEFAULT 0")

                // Copy existing category values to new category_name column
                db.execSQL("UPDATE receipt_analysis SET category_name = category")

                // Set colors for existing categories based on defaults
                CategoryDefaults.defaultCategories.forEach { cat ->
                    db.execSQL(
                        "UPDATE receipt_analysis SET category_color = ${cat.color} WHERE category_name = '${cat.name}'"
                    )
                }

                // Set default color for any unknown categories that still have color = 0
                db.execSQL("UPDATE receipt_analysis SET category_color = 0 WHERE category_color = 0 AND category_name NOT IN (${CategoryDefaults.defaultCategories.joinToString(",") { "'${it.name}'" }})")

                // Drop the existing index first
                db.execSQL("DROP INDEX IF EXISTS index_receipt_analysis_imageUri")

                // Create new table without the old category column
                db.execSQL("""
                    CREATE TABLE receipt_analysis_new (
                        imageUri TEXT NOT NULL PRIMARY KEY,
                        category_name TEXT NOT NULL,
                        category_color INTEGER NOT NULL,
                        finalPrice INTEGER NOT NULL,
                        date TEXT,
                        FOREIGN KEY(imageUri) REFERENCES images(uri) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Copy data to new table
                db.execSQL("""
                    INSERT INTO receipt_analysis_new (imageUri, category_name, category_color, finalPrice, date)
                    SELECT imageUri, category_name, category_color, finalPrice, date FROM receipt_analysis
                """.trimIndent())

                // Drop old table and rename new one
                db.execSQL("DROP TABLE receipt_analysis")
                db.execSQL("ALTER TABLE receipt_analysis_new RENAME TO receipt_analysis")
                
                // Recreate the index on the final table
                db.execSQL("CREATE INDEX index_receipt_analysis_imageUri ON receipt_analysis(imageUri)")
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "palbudget.db"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
