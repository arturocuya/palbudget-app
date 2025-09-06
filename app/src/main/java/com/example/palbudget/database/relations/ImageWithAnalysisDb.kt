package com.example.palbudget.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.palbudget.data.ImageInfo
import com.example.palbudget.data.ReceiptAnalysisEntity
import com.example.palbudget.data.ReceiptItemEntity

data class ImageWithAnalysisDb(
    @Embedded val image: ImageInfo,

    @Relation(
        parentColumn = "uri",
        entityColumn = "imageUri"
    )
    val analysis: ReceiptAnalysisEntity?,

    @Relation(
        parentColumn = "uri",
        entityColumn = "imageUri"
    )
    val items: List<ReceiptItemEntity>
)
