package com.example.palbudget.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptAnalysis(
    val items: List<ReceiptItem>,
    val category: String,
    @SerialName("final_price")
    val finalPrice: Int,
    val date: String?
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
    val category: String,
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
