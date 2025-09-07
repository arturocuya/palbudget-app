package com.example.palbudget.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.palbudget.R
import com.example.palbudget.viewmodel.ImageWithAnalysis

@Composable
fun ScanScreen(
    images: List<ImageWithAnalysis>,
    selectedImages: Set<String>,
    onTakePhoto: () -> Unit,
    onPickMultiple: () -> Unit,
    onRemoveAll: () -> Unit,
    onImageSelected: (String, Boolean) -> Unit,
    isInSelectionMode: Boolean = false,
    isAnalyzing: Boolean = false
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (!isInSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        if (!isAnalyzing) {
                            showBottomSheet = true
                        }
                    }
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.add_24px),
                            contentDescription = "Add photos"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (images.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📷",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = LocalContext.current.getString(R.string.no_images_yet),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = LocalContext.current.getString(R.string.tap_camera_button_to_add),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(images) { imageWithAnalysis ->
                        ImageCard(
                            imageWithAnalysis = imageWithAnalysis,
                            isSelected = selectedImages.contains(imageWithAnalysis.imageInfo.uri),
                            isInSelectionMode = isInSelectionMode,
                            onSelectionChanged = { isSelected ->
                                onImageSelected(imageWithAnalysis.imageInfo.uri, isSelected)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ImageOptionsBottomSheet(
            onDismiss = { showBottomSheet = false },
            onTakePhoto = {
                showBottomSheet = false
                onTakePhoto()
            },
            onPickMultiple = {
                showBottomSheet = false
                onPickMultiple()
            },
            onRemoveAll = {
                showBottomSheet = false
                onRemoveAll()
            }
        )
    }
}
