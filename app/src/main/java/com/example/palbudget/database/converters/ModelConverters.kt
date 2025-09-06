package com.example.palbudget.database.converters

import com.example.palbudget.data.ImageInfo
import com.example.palbudget.data.ReceiptAnalysisEntity
import com.example.palbudget.data.ReceiptItemEntity
import com.example.palbudget.data.ReceiptAnalysis
import com.example.palbudget.data.ReceiptItem
import com.example.palbudget.database.relations.ImageWithAnalysisDb
import com.example.palbudget.viewmodel.ImageWithAnalysis

// ImageInfo is used directly, no conversion needed

// ReceiptAnalysis <-> ReceiptAnalysisEntity
fun ReceiptAnalysis.toEntity(imageUri: String): ReceiptAnalysisEntity = ReceiptAnalysisEntity(
    imageUri = imageUri,
    category = category,
    finalPrice = finalPrice,
    date = date
)

fun ReceiptAnalysisEntity.toDomain(items: List<ReceiptItemEntity>): ReceiptAnalysis = ReceiptAnalysis(
    items = items.map { it.toDomain() },
    category = category,
    finalPrice = finalPrice,
    date = date
)

// ReceiptItem <-> ReceiptItemEntity
fun ReceiptItem.toEntity(imageUri: String): ReceiptItemEntity = ReceiptItemEntity(
    imageUri = imageUri,
    name = name,
    price = price
)

fun ReceiptItemEntity.toDomain(): ReceiptItem = ReceiptItem(
    name = name,
    price = price
)

// ImageWithAnalysisDb to domain
fun ImageWithAnalysisDb.toDomain(): ImageWithAnalysis = ImageWithAnalysis(
    imageInfo = image,
    analysis = analysis?.toDomain(items)
)

// Updated ImageInfo with isReceipt
fun ImageInfo.withReceiptStatus(isReceipt: Boolean): ImageInfo = copy(isReceipt = isReceipt)
