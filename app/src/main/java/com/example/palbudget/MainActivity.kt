package com.example.palbudget

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.palbudget.data.ImageRepository
import com.example.palbudget.service.OpenAIService
import com.example.palbudget.ui.theme.PalBudgetTheme
import com.example.palbudget.utils.ImageUtils
import com.example.palbudget.viewmodel.ImageViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ImageViewModel by viewModels()
    private lateinit var imageRepository: ImageRepository
    
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickSingleImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickMultipleImagesLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    
    private var tempCameraUri: Uri? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        imageRepository = ImageRepository(this)
        setupImageLaunchers()
        
        enableEdgeToEdge()
        setContent {
            PalBudgetTheme {
                PalBudgetApp(
                    images = viewModel.images,
                    onLoadImages = { viewModel.loadImages(it) },
                    onTakePhoto = ::launchCamera,
                    onPickMultiple = ::launchMultipleImagePicker,
                    onRemoveAll = ::removeAllImages,
                    onRemoveSelected = { imageInfos ->
                        imageInfos.forEach { viewModel.removeImage(it) }
                    },
                    onAnalyzeSelected = ::analyzeSelectedImages
                )
            }
        }
    }
    
    private fun setupImageLaunchers() {
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && tempCameraUri != null) {
                val imageInfo = ImageUtils.uriToImageInfo(this, tempCameraUri!!)
                viewModel.addImages(listOf(imageInfo))
                Toast.makeText(this, getString(R.string.photo_captured_successfully), Toast.LENGTH_SHORT).show()
            } else {
                Log.w("PalBudget", "Camera capture failed - success: $success, uri: $tempCameraUri")
            }
            tempCameraUri = null
        }
        
        pickSingleImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            uri?.let {
                val imageInfo = ImageUtils.uriToImageInfo(this, it)
                viewModel.addImages(listOf(imageInfo))
            }
        }
        
        pickMultipleImagesLauncher = registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
        ) { uris ->
            if (uris.isNotEmpty()) {
                // Take persistable URI permissions
                uris.forEach { uri ->
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri, 
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        Log.d("PalBudget", "Could not take persistable permission for $uri", e)
                        Toast.makeText(this@MainActivity, getString(R.string.some_images_may_not_persist), Toast.LENGTH_LONG).show()
                    }
                }
                
                val imageInfos = uris.map { ImageUtils.uriToImageInfo(this, it) }
                viewModel.addImages(imageInfos)
            }
        }
    }
    
    fun launchCamera() {
        val uri = ImageUtils.createImageUri(this)
        uri?.let {
            tempCameraUri = it
            takePictureLauncher.launch(it)
        } ?: run {
            Log.e("PalBudget", "Failed to create camera URI")
            Toast.makeText(this, getString(R.string.failed_to_create_camera_uri), Toast.LENGTH_LONG).show()
        }
    }
    
    private fun launchSingleImagePicker() {
        pickSingleImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    
    private fun launchMultipleImagePicker() {
        pickMultipleImagesLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    
    private fun removeAllImages() {
        viewModel.removeAllImages()
        Toast.makeText(this, getString(R.string.all_images_removed), Toast.LENGTH_SHORT).show()
    }
    
    private fun analyzeSelectedImages(selectedImagesUris: Set<String>) {
        val selectedImages = viewModel.images.filter { selectedImagesUris.contains(it.uri) }
        
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "No images selected for analysis", Toast.LENGTH_LONG).show()
            return
        }
        
        // Convert image URIs to base64 format
        val imageBase64List = selectedImages.mapNotNull { imageInfo ->
            ImageUtils.uriToBase64(this, Uri.parse(imageInfo.uri))
        }
        
        // Launch coroutine to perform analysis
        val openAIService = OpenAIService(this)
        
        if (imageBase64List.isEmpty()) {
            Toast.makeText(this, "Failed to convert images to base64", Toast.LENGTH_LONG).show()
            return
        }
        
        // Show loading toast
        Toast.makeText(this, "Analyzing ${imageBase64List.size} image(s)...", Toast.LENGTH_SHORT).show()
        
        // This should be done in a proper coroutine scope, but for simplicity we'll use GlobalScope
        kotlinx.coroutines.GlobalScope.launch {
            val result = openAIService.analyzeReceipts(imageBase64List)
            
            // Show result on main thread
            runOnUiThread {
                if (result.success) {
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                } else {
                    val errorMessage = result.error ?: "Unknown error occurred"
                    Toast.makeText(this@MainActivity, "Analysis failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalBudgetApp(
    images: List<com.example.palbudget.data.ImageInfo>,
    onLoadImages: (List<com.example.palbudget.data.ImageInfo>) -> Unit,
    onTakePhoto: () -> Unit,
    onPickMultiple: () -> Unit,
    onRemoveAll: () -> Unit,
    onRemoveSelected: (List<com.example.palbudget.data.ImageInfo>) -> Unit,
    onAnalyzeSelected: (Set<String>) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf("receipts") }
    var selectedImages by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    
    // Handle back button when images are selected
    BackHandler(enabled = selectedImages.isNotEmpty()) {
        selectedImages = setOf()
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "PalBudget",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Receipts") },
                        selected = currentPage == "receipts",
                        onClick = {
                            currentPage = "receipts"
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                }
            }
        }
    ) {
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
                        IconButton(
                            onClick = {
                                if (selectedImages.isNotEmpty()) {
                                    selectedImages = setOf()
                                } else {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }
                            }
                        ) {
                            if (selectedImages.isNotEmpty()) {
                                Icon(Icons.Default.Menu, contentDescription = "Clear selection")
                            } else {
                                Icon(Icons.Filled.Menu, contentDescription = "Open menu")
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
                        onRemoveSelected = onRemoveSelected,
                        onImageSelected = { imageId, isSelected ->
                            if (isSelected) {
                                selectedImages = selectedImages + imageId
                            } else {
                                selectedImages = selectedImages - imageId
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
                            val imagesToRemove = images.filter { selectedImages.contains(it.uri) }
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
}

@Composable
fun ReceiptsScreen(
    images: List<com.example.palbudget.data.ImageInfo>,
    selectedImages: Set<String>,
    onLoadImages: (List<com.example.palbudget.data.ImageInfo>) -> Unit,
    onTakePhoto: () -> Unit,
    onPickMultiple: () -> Unit,
    onRemoveAll: () -> Unit,
    onRemoveSelected: (List<com.example.palbudget.data.ImageInfo>) -> Unit,
    onImageSelected: (String, Boolean) -> Unit,
    isInSelectionMode: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Load images on startup
    LaunchedEffect(Unit) {
        val imageRepository = ImageRepository(context)
        val savedImages = imageRepository.loadImages()
        onLoadImages(savedImages)
    }
    
    // Save images when they change
    LaunchedEffect(images.size) {
        val imageRepository = ImageRepository(context)
        imageRepository.saveImages(images)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showBottomSheet = true
                }
            ) {
                Text("\uD83D\uDCF7")
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
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "\uD83D\uDCF7",
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
                    val sortedImages = images.sortedByDescending { it.dateCreated }
                    items(sortedImages) { imageInfo ->
                        ImageCard(
                            imageInfo = imageInfo,
                            isSelected = selectedImages.contains(imageInfo.uri),
                            isInSelectionMode = isInSelectionMode,
                            onSelectionChanged = { isSelected ->
                                onImageSelected(imageInfo.uri, isSelected)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCard(
    imageInfo: com.example.palbudget.data.ImageInfo,
    isSelected: Boolean = false,
    isInSelectionMode: Boolean = false,
    onSelectionChanged: (Boolean) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = android.net.Uri.parse(imageInfo.uri),
                contentDescription = LocalContext.current.getString(R.string.selected_image),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = {
                            if (isInSelectionMode) {
                                onSelectionChanged(!isSelected)
                            }
                        },
                        onLongClick = {
                            if (!isInSelectionMode) {
                                onSelectionChanged(!isSelected)
                            }
                        }
                    ),
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
            )
            
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                )
                
                // Selection checkmark
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
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
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
            onRemoveSelected = { },
            onImageSelected = { _, _ -> },
            isInSelectionMode = false
        )
    }
}
