package com.example.palbudget.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptCategory(
    val name: String,
    val color: Int,
    val description: String? = null
)

object CategoryDefaults {
    val defaultCategories = listOf(
        ReceiptCategory("groceries", 0xFF2E7D32.toInt(), "Food, beverages, household items, and everyday essentials"),
        ReceiptCategory("health", 0xFF1565C0.toInt(), "Medical expenses, pharmacy, healthcare services, and wellness"),
        ReceiptCategory("entertainment", 0xFF7B1FA2.toInt(), "Movies, games, books, streaming services, and leisure activities"),
        ReceiptCategory("restaurant", 0xFFD84315.toInt(), "Dining out, takeout, delivery, and food services"),
        ReceiptCategory("coffee", 0xFF5D4037.toInt(), "Coffee shops, cafes, and specialty beverages")
    )
    
    fun find(name: String): ReceiptCategory? =
        defaultCategories.firstOrNull { it.name.equals(name, ignoreCase = true) }
}

@Serializable
data class ReceiptAnalysisNetwork(
    val items: List<ReceiptItem>,
    val category: String,
    @SerialName("final_price")
    val finalPrice: Int,
    val date: String?
)

data class ReceiptAnalysis(
    val items: List<ReceiptItem>,
    val category: ReceiptCategory,
    val finalPrice: Int,
    val date: String?
)

fun ReceiptAnalysisNetwork.toDomain(): ReceiptAnalysis =
    ReceiptAnalysis(
        items = items,
        category = CategoryDefaults.find(category) ?: ReceiptCategory(category, 0, null),
        finalPrice = finalPrice,
        date = date
    )

@Serializable
data class ReceiptItem(
    val name: String,
    val price: Int
)

@Entity(
    tableName = "receipt_analysis",
    foreignKeys = [
        ForeignKey(
            entity = ImageInfo::class,
            parentColumns = ["uri"],
            childColumns = ["imageUri"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("imageUri")]
)
data class ReceiptAnalysisEntity(
    @PrimaryKey val imageUri: String,
    @Embedded(prefix = "category_")
    val category: ReceiptCategory,
    val finalPrice: Int,
    val date: String?
)

@Entity(
    tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptAnalysisEntity::class,
            parentColumns = ["imageUri"],
            childColumns = ["imageUri"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("imageUri")]
)
data class ReceiptItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val name: String,
    val price: Int
)
