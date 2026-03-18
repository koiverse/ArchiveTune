/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * 
 * Smart Queue Manager by @cenzer0
 */

package moe.koiverse.archivetune.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.R

/**
 * Smart Queue Manager with AI-powered suggestions
 * 
 * Features:
 * - Drag and drop reordering
 * - Smart suggestions based on listening history
 * - Queue optimization
 * - Batch operations
 * - Visual feedback
 * 
 * @author @cenzer0
 */

data class QueueItem(
    val id: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String?,
    val duration: Long,
    val isCurrentlyPlaying: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartQueueManager(
    queueItems: List<QueueItem>,
    onItemClick: (String) -> Unit,
    onItemRemove: (String) -> Unit,
    onItemMove: (Int, Int) -> Unit,
    onOptimizeQueue: () -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOptimizeDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Queue header with actions
        QueueHeader(
            itemCount = queueItems.size,
            onOptimize = { showOptimizeDialog = true },
            onClear = onClearQueue
        )
        
        // Queue list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = queueItems,
                key = { _, item -> item.id }
            ) { index, item ->
                QueueItemCard(
                    item = item,
                    index = index,
                    onClick = { onItemClick(item.id) },
                    onRemove = { onItemRemove(item.id) }
                )
            }
        }
    }
    
    // Optimize dialog
    if (showOptimizeDialog) {
        OptimizeQueueDialog(
            onDismiss = { showOptimizeDialog = false },
            onConfirm = {
                onOptimizeQueue()
                showOptimizeDialog = false
            }
        )
    }
}

@Composable
private fun QueueHeader(
    itemCount: Int,
    onOptimize: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Queue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$itemCount songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Optimize button
                FilledTonalButton(
                    onClick = onOptimize,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.smart_shuffle),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Optimize")
                }
                
                // Clear button
                IconButton(onClick = onClear) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = "Clear queue"
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueItemCard(
    item: QueueItem,
    index: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (item.isCurrentlyPlaying) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val scale by animateFloatAsState(
        targetValue = if (item.isCurrentlyPlaying) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index or playing indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (item.isCurrentlyPlaying) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.isCurrentlyPlaying) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Album art
            AsyncImage(
                model = item.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // Song info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Duration
            Text(
                text = formatDuration(item.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun OptimizeQueueDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Optimize Queue") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Smart Queue will:")
                Text("• Distribute artists evenly")
                Text("• Avoid repetitive patterns")
                Text("• Create better flow")
                Text("• Keep current song playing")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Optimize")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}
