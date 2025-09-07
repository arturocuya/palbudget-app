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
import com.example.palbudget.ui.theme.PalBudgetTheme
import com.example.palbudget.utils.ImageUtils
import com.example.palbudget.viewmodel.ImageWithAnalysis
import com.example.palbudget.viewmodel.ReceiptsViewModel
import com.example.palbudget.viewmodel.ScanViewModel
import com.example.palbudget.views.MainScreen

class MainActivity : ComponentActivity() {
    private val receiptsViewModel: ReceiptsViewModel by viewModels()
    private val scanViewModel: ScanViewModel by viewModels()

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickSingleImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickMultipleImagesLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    private var tempCameraUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupImageLaunchers()

        enableEdgeToEdge()
        setContent {
            PalBudgetTheme {
                MainScreen(
                    scanImages = scanViewModel.images,
                    receipts = receiptsViewModel.receipts,
                    onTakePhoto = ::launchCamera,
                    onPickMultiple = ::launchMultipleImagePicker,
                    onRemoveAllScan = ::removeAllScanImages,
                    onRemoveSelectedScan = { imageWithAnalysisList ->
                        scanViewModel.removeImages(imageWithAnalysisList)
                    },
                    onRemoveSelectedReceipts = { imageWithAnalysisList ->
                        imageWithAnalysisList.forEach { receiptsViewModel.removeReceipt(it) }
                    },
                    onAnalyzeSelected = ::analyzeSelectedImages,
                    isAnalyzing = scanViewModel.isAnalyzing
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
                scanViewModel.addImages(listOf(imageInfo))
                Toast.makeText(
                    this,
                    getString(R.string.photo_captured_successfully),
                    Toast.LENGTH_SHORT
                ).show()
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
                scanViewModel.addImages(listOf(imageInfo))
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
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.some_images_may_not_persist),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                val imageInfos = uris.map { ImageUtils.uriToImageInfo(this, it) }
                scanViewModel.addImages(imageInfos)
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
            Toast.makeText(this, getString(R.string.failed_to_create_camera_uri), Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun launchMultipleImagePicker() {
        pickMultipleImagesLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun removeAllScanImages() {
        scanViewModel.removeAllImages()
        Toast.makeText(this, "All scan images removed", Toast.LENGTH_SHORT).show()
    }

    private fun analyzeSelectedImages(selectedImagesUris: Set<String>) {
        scanViewModel.analyzeSelected(this, selectedImagesUris)
    }


}

