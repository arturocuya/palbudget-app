package com.example.palbudget.data

import kotlinx.serialization.Serializable

@Serializable
data class ImageInfo(
    val uri: String,
    val dateCreated: Long
)