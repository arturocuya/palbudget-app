package com.example.palbudget.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.palbudget.data.ImageInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "image_preferences")

class ImageRepository(private val context: Context) {
    
    private val imagesKey = stringPreferencesKey("images_json")
    
    suspend fun saveImages(images: List<ImageInfo>) {
        val json = Json.encodeToString(images)
        context.dataStore.edit { preferences ->
            preferences[imagesKey] = json
        }
    }
    
    suspend fun loadImages(): List<ImageInfo> {
        val json = context.dataStore.data.map { preferences ->
            preferences[imagesKey] ?: "[]"
        }.first()
        
        return try {
            Json.decodeFromString<List<ImageInfo>>(json)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
