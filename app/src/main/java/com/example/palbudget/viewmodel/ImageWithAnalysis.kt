package com.example.palbudget.viewmodel

import com.example.palbudget.data.ImageInfo
import com.example.palbudget.data.ReceiptAnalysis

data class ImageWithAnalysis(
    val imageInfo: ImageInfo,
    val analysis: ReceiptAnalysis?
)
