package com.example.palbudget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.palbudget.database.AppDatabase
import com.example.palbudget.repository.RoomImageRepository

abstract class BaseImageViewModel(application: Application) : AndroidViewModel(application) {
    
    protected val repository: RoomImageRepository by lazy {
        val database = AppDatabase.getDatabase(application)
        RoomImageRepository(database.imageDao())
    }
}
