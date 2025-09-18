package com.example.palbudget.views

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.palbudget.R
import com.example.palbudget.viewmodel.ImageWithAnalysis
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class TimeGroup {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    OLDER
}

data class GroupedReceipt(
    val timeGroup: TimeGroup,
    val monthYear: String? = null,
    val receipt: ImageWithAnalysis
)

private fun parseReceiptDate(dateString: String?): LocalDate? {
    if (dateString == null) return null

    return try {
        val possibleFormats = listOf(
            "yyyy-MM-dd",
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "yyyy/MM/dd"
        )

        for (format in possibleFormats) {
            try {
                val formatter = DateTimeFormatter.ofPattern(format)
                return LocalDate.parse(dateString, formatter)
            } catch (e: Exception) {
                continue
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

private fun categorizeReceipt(receipt: ImageWithAnalysis): GroupedReceipt {
    val receiptDate = parseReceiptDate(receipt.analysis?.date)
    val today = LocalDate.now()

    if (receiptDate == null) {
        return GroupedReceipt(TimeGroup.OLDER, receipt = receipt)
    }

    return when {
        receiptDate.isEqual(today) -> GroupedReceipt(TimeGroup.TODAY, receipt = receipt)
        receiptDate.isEqual(today.minusDays(1)) -> GroupedReceipt(
            TimeGroup.YESTERDAY,
            receipt = receipt
        )

        receiptDate.isAfter(today.minusDays(7)) -> GroupedReceipt(
            TimeGroup.THIS_WEEK,
            receipt = receipt
        )

        receiptDate.year == today.year && receiptDate.month == today.month ->
            GroupedReceipt(TimeGroup.THIS_MONTH, receipt = receipt)

        else -> {
            val monthYear = if (receiptDate.year == today.year) {
                receiptDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            } else {
                "${
                    receiptDate.month.getDisplayName(
                        TextStyle.FULL,
                        Locale.getDefault()
                    )
                } ${receiptDate.year}"
            }
            GroupedReceipt(TimeGroup.OLDER, monthYear, receipt)
        }
    }
}

private fun groupReceipts(receipts: List<ImageWithAnalysis>): Map<String, List<ImageWithAnalysis>> {
    val grouped = receipts.map { categorizeReceipt(it) }
        .groupBy { groupedReceipt ->
            when (groupedReceipt.timeGroup) {
                TimeGroup.TODAY -> "Today"
                TimeGroup.YESTERDAY -> "Yesterday"
                TimeGroup.THIS_WEEK -> "This Week"
                TimeGroup.THIS_MONTH -> LocalDate.now().month.getDisplayName(
                    TextStyle.FULL,
                    Locale.getDefault()
                )

                TimeGroup.OLDER -> groupedReceipt.monthYear ?: "Unknown Date"
            }
        }

    val sortedKeys = grouped.keys.sortedWith { key1, key2 ->
        val priority = mapOf(
            "Unknown Date" to 0,
            "Today" to 1,
            "Yesterday" to 2,
            "This Week" to 3,
            LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault()) to 4
        )

        val p1 = priority[key1] ?: 1000
        val p2 = priority[key2] ?: 1000

        if (p1 != 1000 || p2 != 1000) {
            p1.compareTo(p2)
        } else {
            // For month/year entries, sort by date descending
            key2.compareTo(key1)
        }
    }

    return sortedKeys.associateWith { key ->
        grouped[key]?.map { it.receipt }?.sortedByDescending { receipt ->
            parseReceiptDate(receipt.analysis?.date)
        } ?: emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen(
    receipts: List<ImageWithAnalysis>,
    onOpenImage: (String) -> Unit
) {
    // NOTE: This screen only shows analyzed receipts from the database
    // No scanning functionality - that's handled by ScanScreen

    var selectedReceipt by remember { mutableStateOf<ImageWithAnalysis?>(null) }

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
                        Icon(
                            painter = painterResource(id = R.drawable.receipt_long_off_24px),
                            modifier = Modifier.size(64.dp),
                            contentDescription = "Add receipts"
                        )
                        Text(
                            text = stringResource(R.string.no_receipts_yet),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.scan_receipts_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }

                return@Surface
            }

            val groupedReceipts = groupReceipts(receipts)

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedReceipts.forEach { (sectionTitle, sectionReceipts) ->
                    if (sectionReceipts.isNotEmpty()) {
                        item(key = "header_$sectionTitle") {
                            Text(
                                text = sectionTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        item(key = "grid_$sectionTitle") {
                            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                            val minItemWidth = 110.dp
                            val spacing = 8.dp
                            val padding = 32.dp // Account for LazyColumn padding
                            val itemsPerRow =
                                ((screenWidth - padding) / (minItemWidth + spacing)).toInt()
                                    .coerceAtLeast(1)

                            val rows = sectionReceipts.chunked(itemsPerRow)

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rows.forEach { rowItems ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rowItems.forEach { imageWithAnalysis ->
                                            Box(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                ImageCard(
                                                    imageWithAnalysis = imageWithAnalysis,
                                                    isSelected = false,
                                                    isInSelectionMode = false,
                                                    onSelectionChanged = {},
                                                    showAnalysisIcon = false,
                                                    onImageClick = {
                                                        selectedReceipt =
                                                            imageWithAnalysis
                                                    }
                                                )
                                            }
                                        }
                                        // Fill remaining space if row is not complete
                                        repeat(itemsPerRow - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    // Analysis details bottom sheet
    selectedReceipt?.let { receipt ->
        receipt.analysis?.let { analysis ->
            ModalBottomSheet(
                onDismissRequest = { selectedReceipt = null },
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                ReceiptAnalysisBottomSheet(
                    imageWithAnalysis = receipt,
                    onOpenImage = onOpenImage
                )
            }
        }
    }
}

@Composable
fun ReceiptAnalysisBottomSheet(
    imageWithAnalysis: ImageWithAnalysis,
    onOpenImage: (String) -> Unit
) {
    val analysis = imageWithAnalysis.analysis!!
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.receipt_details),
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
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = analysis.category.name.uppercase(),
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
                text = stringResource(R.string.date_label),
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
                text = stringResource(R.string.total_label),
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
                text = stringResource(R.string.items_count, analysis.items.size),
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
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenImage(imageWithAnalysis.imageInfo.uri)
                },
            contentScale = ContentScale.FillWidth
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
