package com.example.palbudget.data

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
