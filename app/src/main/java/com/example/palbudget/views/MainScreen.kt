package com.example.palbudget.views

import com.example.palbudget.viewmodel.ImageWithAnalysis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.palbudget.R
import com.example.palbudget.data.ImageInfo
import com.example.palbudget.ui.theme.PalBudgetTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    images: List<ImageWithAnalysis>,
    onLoadImages: (List<ImageInfo>) -> Unit,
    onTakePhoto: () -> Unit,
    onPickMultiple: () -> Unit,
    onRemoveAll: () -> Unit,
    onRemoveSelected: (List<ImageWithAnalysis>) -> Unit,
    onAnalyzeSelected: (Set<String>) -> Unit
) {
    var currentPage by remember { mutableStateOf("receipts") }
    var selectedImages by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Handle back button when images are selected
    BackHandler(enabled = selectedImages.isNotEmpty()) {
        selectedImages = setOf()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedImages.isNotEmpty()) {
                        Text("${selectedImages.size} selected")
                    } else {
                        Text("PalBudget")
                    }
                },
                navigationIcon = {
                    if (selectedImages.isNotEmpty()) {
                        IconButton(
                            onClick = { selectedImages = setOf() }
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Clear selection")
                        }
                    }
                },
                actions = {
                    if (selectedImages.isNotEmpty()) {
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true }
                            ) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Analyze") },
                                    onClick = {
                                        showOverflowMenu = false
                                        onAnalyzeSelected(selectedImages)
                                        selectedImages = setOf()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove") },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteConfirmation = true
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Receipts") },
                    selected = currentPage == "receipts",
                    onClick = { currentPage = "receipts" }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentPage) {
                "receipts" -> ReceiptsScreen(
                    images = images,
                    selectedImages = selectedImages,
                    onLoadImages = onLoadImages,
                    onTakePhoto = onTakePhoto,
                    onPickMultiple = onPickMultiple,
                    onRemoveAll = onRemoveAll,
                    onImageSelected = { imageId, isSelected ->
                        selectedImages = if (isSelected) {
                            selectedImages + imageId
                        } else {
                            selectedImages - imageId
                        }
                    },
                    isInSelectionMode = selectedImages.isNotEmpty()
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text("Remove ${selectedImages.size} receipt${if (selectedImages.size == 1) "" else "s"}")
            },
            text = {
                Text(LocalContext.current.getString(R.string.remove_all_images_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        // Remove selected images
                        val imagesToRemove = images.filter { selectedImages.contains(it.imageInfo.uri) }
                        onRemoveSelected(imagesToRemove)
                        selectedImages = setOf()
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text(LocalContext.current.getString(R.string.cancel))
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ImageOptionsBottomSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickMultiple: () -> Unit,
    onRemoveAll: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var showRemoveAllDialog by remember { mutableStateOf(false) }

    // Auto-launch camera when permission is granted
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            // Small delay to prevent immediate execution
            kotlinx.coroutines.delay(100)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = LocalContext.current.getString(R.string.add_photos),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Take Photo Option with Permission Handling
            OutlinedButton(
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        onTakePhoto()
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(LocalContext.current.getString(R.string.take_photo))
                }
            }

            // Choose from Gallery (Multiple)
            OutlinedButton(
                onClick = onPickMultiple,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(LocalContext.current.getString(R.string.choose_from_gallery))
                }
            }

            // Remove All Images
            OutlinedButton(
                onClick = { showRemoveAllDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(LocalContext.current.getString(R.string.remove_all_images_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation Dialog for Remove All
    if (showRemoveAllDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveAllDialog = false },
            title = { Text(LocalContext.current.getString(R.string.remove_all_images_title)) },
            text = {
                Text(LocalContext.current.getString(R.string.remove_all_images_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveAllDialog = false
                        onRemoveAll()
                    }
                ) {
                    Text(LocalContext.current.getString(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveAllDialog = false }
                ) {
                    Text(LocalContext.current.getString(R.string.cancel))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReceiptsScreenPreview() {
    PalBudgetTheme {
        ReceiptsScreen(
            images = emptyList(),
            selectedImages = setOf(),
            onLoadImages = { },
            onTakePhoto = { },
            onPickMultiple = { },
            onRemoveAll = { },
            onImageSelected = { _, _ -> },
            isInSelectionMode = false
        )
    }
}
