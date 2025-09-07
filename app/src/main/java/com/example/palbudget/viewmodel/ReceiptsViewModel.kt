package com.example.palbudget.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ReceiptsViewModel(application: Application) : BaseImageViewModel(application) {

    companion object {
        private const val TAG = "ReceiptsViewModel"
    }

    private val _receipts = mutableStateListOf<ImageWithAnalysis>()
    val receipts: SnapshotStateList<ImageWithAnalysis> = _receipts

    init {
        
        // Observe only the receipts (analyzed images) from the database
        viewModelScope.launch {
            repository.receipts.collect { receiptsList ->
                Log.d(TAG, "Received ${receiptsList.size} receipts from repository")
                _receipts.clear()
                _receipts.addAll(receiptsList)
            }
        }
    }



    fun removeReceipt(receipt: ImageWithAnalysis) {
        viewModelScope.launch {
            try {
                repository.removeImage(receipt)
                Log.d(TAG, "Removed receipt: ${receipt.imageInfo.uri}")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing receipt", e)
            }
        }
    }

    fun removeSelected(receiptsToRemove: List<ImageWithAnalysis>) {
        viewModelScope.launch {
            try {
                receiptsToRemove.forEach { repository.removeImage(it) }
                Log.d(TAG, "Removed ${receiptsToRemove.size} receipts")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing receipts", e)
            }
        }
    }

    fun removeAllReceipts() {
        viewModelScope.launch {
            try {
                repository.removeAllImages()
                Log.d(TAG, "Removed all receipts")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing all receipts", e)
            }
        }
    }
}
