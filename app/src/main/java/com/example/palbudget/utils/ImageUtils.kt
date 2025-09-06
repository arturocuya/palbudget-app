package com.example.palbudget.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import com.example.palbudget.data.ImageInfo
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {
    
    fun createImageUri(context: Context): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_${timeStamp}.jpg"
            
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures")
            }
            
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        } catch (e: Exception) {
            android.util.Log.e("PalBudget", "Failed to create image URI", e)
            null
        }
    }
    
    fun getImageDateFromUri(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media.DATE_ADDED),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    cursor.getLong(dateIndex) * 1000 // Convert to milliseconds
                } else {
                    System.currentTimeMillis()
                }
            } ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    fun uriToImageInfo(context: Context, uri: Uri): ImageInfo {
        return ImageInfo(
            uri = uri.toString(),
            dateCreated = getImageDateFromUri(context, uri)
        )
    }

    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
            }
        } catch (e: Exception) {
            android.util.Log.e("PalBudget", "Failed to convert URI to base64: $uri", e)
            null
        }
    }
}
