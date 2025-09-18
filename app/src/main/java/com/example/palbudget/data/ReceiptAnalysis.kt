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
    val color: Int
)

object CategoryDefaults {
    val defaultCategories = listOf(
        ReceiptCategory("groceries", 0xFF4CAF50.toInt()),
        ReceiptCategory("health", 0xFF2196F3.toInt()),
        ReceiptCategory("entertainment", 0xFFFFC107.toInt()),
        ReceiptCategory("restaurant", 0xFFF44336.toInt())
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
        category = CategoryDefaults.find(category) ?: ReceiptCategory(category, 0),
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
