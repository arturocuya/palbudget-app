package com.example.palbudget.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "images")
@Serializable
data class ImageInfo(
    @PrimaryKey val uri: String,
    val dateCreated: Long,
    val isReceipt: Boolean? = null
)