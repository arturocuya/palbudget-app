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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.palbudget.data.ImageRepository
import com.example.palbudget.ui.theme.PalBudgetTheme
import com.example.palbudget.utils.ImageUtils
import com.example.palbudget.viewmodel.ImageViewModel
import kotlinx.coroutines.launch

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
                    onRemoveAll = ::removeAllImages
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalBudgetApp(
    images: List<com.example.palbudget.data.ImageInfo>,
    onLoadImages: (List<com.example.palbudget.data.ImageInfo>) -> Unit,
    onTakePhoto: () -> Unit,
    onPickMultiple: () -> Unit,
    onRemoveAll: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf("receipts") }
    
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
                    title = { Text("PalBudget") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open menu")
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
                        onLoadImages = onLoadImages,
                        onTakePhoto = onTakePhoto,
                        onPickMultiple = onPickMultiple,
                        onRemoveAll = onRemoveAll
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptsScreen(
    images: List<com.example.palbudget.data.ImageInfo>,
    onLoadImages: (List<com.example.palbudget.data.ImageInfo>) -> Unit,
    onTakePhoto: () -> Unit,
    onPickMultiple: () -> Unit,
    onRemoveAll: () -> Unit
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
                        ImageCard(imageInfo = imageInfo)
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

@Composable
fun ImageCard(imageInfo: com.example.palbudget.data.ImageInfo) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AsyncImage(
            model = android.net.Uri.parse(imageInfo.uriString),
            contentDescription = LocalContext.current.getString(R.string.selected_image),
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
            error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
            placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
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
            onLoadImages = { },
            onTakePhoto = { },
            onPickMultiple = { },
            onRemoveAll = { }
        )
    }
}
