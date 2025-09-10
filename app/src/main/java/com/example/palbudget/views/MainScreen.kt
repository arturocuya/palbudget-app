package com.example.palbudget.views

import com.example.palbudget.viewmodel.ImageWithAnalysis
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.palbudget.R
import com.example.palbudget.data.ImageInfo
import com.example.palbudget.ui.theme.PalBudgetTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    scanImages: List<ImageWithAnalysis>,
    receipts: List<ImageWithAnalysis>,
    onTakePhoto: () -> Unit,
    onPickMultiple: () -> Unit,
    onRemoveAllScan: () -> Unit,
    onRemoveSelectedScan: (List<ImageWithAnalysis>) -> Unit,
    onRemoveSelectedReceipts: (List<ImageWithAnalysis>) -> Unit,
    onAnalyzeSelected: (Set<String>) -> Unit,
    isAnalyzing: Boolean = false,
    onOpenImage: (String) -> Unit
) {
    var currentDestination by remember { mutableStateOf<NavDestination>(NavDestination.Scan) }
    var selectedImages by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Handle back button when images are selected
    BackHandler(enabled = selectedImages.isNotEmpty()) {
        selectedImages = setOf()
    }
    
    // Clear selection when switching destinations
    LaunchedEffect(currentDestination) {
        selectedImages = setOf()
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { 
                        Icon(
                            painter = painterResource(id = R.drawable.document_scanner_24px),
                            contentDescription = null
                        )
                    },
                    label = { Text("Scan") },
                    selected = currentDestination == NavDestination.Scan,
                    onClick = { currentDestination = NavDestination.Scan }
                )
                NavigationBarItem(
                    icon = { 
                        Icon(
                            painter = painterResource(id = R.drawable.receipt_long_24px),
                            contentDescription = null
                        )
                    },
                    label = { Text("Receipts") },
                    selected = currentDestination == NavDestination.Receipts,
                    onClick = { currentDestination = NavDestination.Receipts }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentDestination) {
                    NavDestination.Scan -> ScanScreen(
                        images = scanImages,
                        selectedImages = selectedImages,
                        onTakePhoto = onTakePhoto,
                        onPickMultiple = onPickMultiple,
                        onRemoveAll = onRemoveAllScan,
                        onImageSelected = { imageId, isSelected ->
                            selectedImages = if (isSelected) {
                                selectedImages + imageId
                            } else {
                                selectedImages - imageId
                            }
                        },
                        isInSelectionMode = selectedImages.isNotEmpty(),
                        isAnalyzing = isAnalyzing
                    )
                    NavDestination.Receipts -> ReceiptsScreen(
                        receipts = receipts,
                        onOpenImage = onOpenImage
                    )
                }
                
                // Selected images bottom sheet (non-modal)
                if (selectedImages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        SelectedImagesBottomSheet(
                            selectedCount = selectedImages.size,
                            isInScanMode = currentDestination == NavDestination.Scan,
                            onSelectAll = {
                            selectedImages = when (currentDestination) {
                            NavDestination.Scan -> scanImages.map { it.imageInfo.uri }.toSet()
                            NavDestination.Receipts -> receipts.map { it.imageInfo.uri }.toSet()
                            }
                            },
                            onAnalyze = {
                                onAnalyzeSelected(selectedImages)
                                selectedImages = setOf()
                            },
                            onRemove = {
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }
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
                        // Remove selected images based on current destination
                        when (currentDestination) {
                            NavDestination.Scan -> {
                                val imagesToRemove = scanImages.filter { selectedImages.contains(it.imageInfo.uri) }
                                onRemoveSelectedScan(imagesToRemove)
                            }
                            NavDestination.Receipts -> {
                                val imagesToRemove = receipts.filter { selectedImages.contains(it.imageInfo.uri) }
                                onRemoveSelectedReceipts(imagesToRemove)
                            }
                        }
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
    var permissionJustRequested by rememberSaveable { mutableStateOf(false) }

    // Auto-launch camera when permission is granted after request
    LaunchedEffect(cameraPermissionState.status.isGranted, permissionJustRequested) {
        if (cameraPermissionState.status.isGranted && permissionJustRequested) {
            onTakePhoto()
            permissionJustRequested = false
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Horizontal row with icons and labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Take Photo Option
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            if (cameraPermissionState.status.isGranted) {
                                onTakePhoto()
                            } else {
                                permissionJustRequested = true
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }
                        .padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add_a_photo_24px),
                        contentDescription = "Take photo",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.take_photo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Choose from Gallery Option
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onPickMultiple() }
                        .padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add_photo_alternate_24px),
                        contentDescription = "Choose from gallery",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.choose_from_gallery),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SelectedImagesBottomSheet(
    selectedCount: Int,
    isInScanMode: Boolean,
    onSelectAll: () -> Unit,
    onAnalyze: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Select All
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelectAll() }
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.select_all_24px),
                    contentDescription = "Select all",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.select_all),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Analyze (only show in scan mode)
            if (isInScanMode) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onAnalyze() }
                        .padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.automation_24px),
                        contentDescription = "Analyze",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.analyze),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Remove Selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onRemove() }
                    .padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.remove_selection_24px),
                    contentDescription = "Remove selected",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.remove),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    PalBudgetTheme {
        MainScreen(
            scanImages = emptyList(),
            receipts = emptyList(),
            onTakePhoto = { },
            onPickMultiple = { },
            onRemoveAllScan = { },
            onRemoveSelectedScan = { },
            onRemoveSelectedReceipts = { },
            onAnalyzeSelected = { },
            onOpenImage = { }
        )
    }
}
