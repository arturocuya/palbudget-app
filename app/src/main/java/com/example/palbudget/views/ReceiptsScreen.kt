package com.example.palbudget.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.palbudget.R
import com.example.palbudget.data.ReceiptAnalysis
import com.example.palbudget.viewmodel.ImageWithAnalysis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen(
    receipts: List<ImageWithAnalysis>,
    selectedImages: Set<String>,
    onImageSelected: (String, Boolean) -> Unit,
    isInSelectionMode: Boolean = false
) {
    // NOTE: This screen only shows analyzed receipts from the database
    // No scanning functionality - that's handled by ScanScreen
    
    var selectedReceiptForAnalysis by remember { mutableStateOf<ImageWithAnalysis?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (receipts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📄",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "No receipts yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Scan some receipts to see them here",
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
                    items(receipts) { imageWithAnalysis ->
                        ImageCard(
                            imageWithAnalysis = imageWithAnalysis,
                            isSelected = selectedImages.contains(imageWithAnalysis.imageInfo.uri),
                            isInSelectionMode = isInSelectionMode,
                            onSelectionChanged = { isSelected ->
                                onImageSelected(imageWithAnalysis.imageInfo.uri, isSelected)
                            },
                            showAnalysisIcon = false, // Hide check marks in receipts screen
                            onImageClick = if (!isInSelectionMode) {
                                { selectedReceiptForAnalysis = imageWithAnalysis }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Analysis details bottom sheet
    selectedReceiptForAnalysis?.let { receipt ->
        receipt.analysis?.let { analysis ->
            ModalBottomSheet(
                onDismissRequest = { selectedReceiptForAnalysis = null },
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                ReceiptAnalysisBottomSheet(
                    imageWithAnalysis = receipt,
                    onDismiss = { selectedReceiptForAnalysis = null }
                )
            }
        }
    }
}

@Composable
fun ReceiptAnalysisBottomSheet(
    imageWithAnalysis: ImageWithAnalysis,
    onDismiss: () -> Unit
) {
    val analysis = imageWithAnalysis.analysis!!
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Receipt Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Category and Total
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Category:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = analysis.category.uppercase(),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Date:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = analysis.date ?: "Not available",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total:",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$${analysis.finalPrice / 100.0}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Items section
        if (analysis.items.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Text(
                text = "Items (${analysis.items.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            analysis.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$${item.price / 100.0}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Receipt image at the bottom
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        
        AsyncImage(
            model = imageWithAnalysis.imageInfo.uri.toUri(),
            contentDescription = "Receipt image",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
