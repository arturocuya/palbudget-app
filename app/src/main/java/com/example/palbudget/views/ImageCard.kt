package com.example.palbudget.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.palbudget.R
import com.example.palbudget.viewmodel.ImageWithAnalysis

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageCard(
    imageWithAnalysis: ImageWithAnalysis,
    isSelected: Boolean = false,
    isInSelectionMode: Boolean = false,
    onSelectionChanged: (Boolean) -> Unit = {},
    showAnalysisIcon: Boolean = true,
    onImageClick: (() -> Unit)? = null
) {
    val imageInfo = imageWithAnalysis.imageInfo
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
                model = imageInfo.uri.toUri(),
                contentDescription = LocalContext.current.getString(R.string.selected_image),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = {
                            if (isInSelectionMode) {
                                onSelectionChanged(!isSelected)
                            } else if (onImageClick != null) {
                                onImageClick()
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

            // Analysis status icon (bottom right) - only show if requested
            if (showAnalysisIcon) {
                imageWithAnalysis.analysis?.let { analysis ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(20.dp)
                            .background(
                                color = if (analysis.items.isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (analysis.items.isNotEmpty()) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (analysis.items.isNotEmpty()) "Receipt detected" else "Not a receipt",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
