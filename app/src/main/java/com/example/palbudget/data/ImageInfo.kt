package com.example.palbudget.data

import kotlinx.serialization.Serializable

@Serializable
data class ImageInfo(
    val uriString: String,
    val dateCreated: Long
)
