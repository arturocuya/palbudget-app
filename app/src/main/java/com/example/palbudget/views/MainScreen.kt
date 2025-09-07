package com.example.palbudget.views

import com.example.palbudget.viewmodel.ImageWithAnalysis
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.painterResource
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
    onAnalyzeSelected: (Set<String>) -> Unit
) {
    var currentDestination by remember { mutableStateOf<NavDestination>(NavDestination.Scan) }
    var selectedImages by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Handle back button when images are selected
    BackHandler(enabled = selectedImages.isNotEmpty()) {
        selectedImages = setOf()
    }
    
    // Clear selection when switching destinations
    LaunchedEffect(currentDestination) {
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
                                if (currentDestination == NavDestination.Scan) {
                                    DropdownMenuItem(
                                        text = { Text("Analyze") },
                                        onClick = {
                                            showOverflowMenu = false
                                            onAnalyzeSelected(selectedImages)
                                            selectedImages = setOf()
                                        }
                                    )
                                }
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
                    isInSelectionMode = selectedImages.isNotEmpty()
                )
                NavDestination.Receipts -> ReceiptsScreen(
                    receipts = receipts,
                    selectedImages = selectedImages,
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
                        text = "Take photo",
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
                        text = "Choose from gallery",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
            onAnalyzeSelected = { }
        )
    }
}
