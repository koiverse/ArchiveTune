package moe.koiverse.archivetune.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.db.MusicDatabase
import moe.koiverse.archivetune.db.entities.EventWithSong
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

/**
 * Listening History Timeline Screen
 * 
 * Beautiful timeline view of listening history inspired by YouTube Music
 * 
 * Features:
 * - Chronological timeline display
 * - Grouped by date (Today, Yesterday, This Week, etc.)
 * - Play count indicators
 * - Time stamps for each play
 * - Listening statistics card
 * - Smooth animations
 * 
 * @author @cenzer0
 */

data class HistoryItem(
    val mediaId: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String?,
    val playedAt: Long,
    val duration: Long
)

data class HistoryGroup(
    val dateLabel: String,
    val items: List<HistoryItem>
)

data class ListeningStats(
    val totalListeningTime: Long,
    val songsPlayed: Int,
    val topGenre: String,
    val averageSessionLength: Long
)

@HiltViewModel
class ListeningHistoryViewModel @Inject constructor(
    database: MusicDatabase
) : ViewModel() {
    val historyItems = database.events()
        .map { events ->
            events.map { eventWithSong ->
                HistoryItem(
                    mediaId = eventWithSong.song.id,
                    title = eventWithSong.song.song.title,
                    artist = eventWithSong.song.artists.joinToString(", ") { it.name },
                    albumArtUrl = eventWithSong.song.song.thumbnailUrl,
                    playedAt = eventWithSong.event.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    duration = eventWithSong.event.playTime
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningHistoryScreen(
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListeningHistoryViewModel = hiltViewModel()
) {
    val historyItems by viewModel.historyItems.collectAsState()
    val groupedHistory = remember(historyItems) { groupHistoryByDate(historyItems) }
    val stats = remember(historyItems) { calculateStats(historyItems) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listening History") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statistics Card
            item {
                ListeningStatsCard(stats = stats)
            }
            
            // Timeline
            groupedHistory.forEach { group ->
                item {
                    DateHeader(dateLabel = group.dateLabel)
                }
                
                items(
                    items = group.items,
                    key = { "${it.mediaId}_${it.playedAt}" }
                ) { item ->
                    HistoryTimelineItem(
                        item = item,
                        onClick = { onItemClick(item.mediaId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningStatsCard(
    stats: ListeningStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your Listening Stats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Total Time",
                    value = formatDuration(stats.totalListeningTime),
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    label = "Songs Played",
                    value = stats.songsPlayed.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Top Genre",
                    value = stats.topGenre,
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    label = "Avg Session",
                    value = formatDuration(stats.averageSessionLength),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun DateHeader(
    dateLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun HistoryTimelineItem(
    item: HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        }
        
        // Content
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            AsyncImage(
                model = item.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // Song Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
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
                
                Text(
                    text = formatTime(item.playedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// Helper functions

private fun groupHistoryByDate(items: List<HistoryItem>): List<HistoryGroup> {
    val calendar = Calendar.getInstance()
    val today = calendar.apply { 
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val yesterday = calendar.apply { 
        add(Calendar.DAY_OF_YEAR, -1) 
    }.timeInMillis
    
    val thisWeek = calendar.apply { 
        set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    }.timeInMillis
    
    return items
        .groupBy { item ->
            when {
                item.playedAt >= today -> "Today"
                item.playedAt >= yesterday -> "Yesterday"
                item.playedAt >= thisWeek -> "This Week"
                else -> {
                    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    dateFormat.format(Date(item.playedAt))
                }
            }
        }
        .map { (label, items) ->
            HistoryGroup(
                dateLabel = label,
                items = items.sortedByDescending { it.playedAt }
            )
        }
        .sortedByDescending { group ->
            group.items.firstOrNull()?.playedAt ?: 0L
        }
}

private fun calculateStats(items: List<HistoryItem>): ListeningStats {
    val totalTime = items.sumOf { it.duration }
    val songsPlayed = items.size
    
    // Group by date to calculate sessions
    val sessions = items
        .sortedBy { it.playedAt }
        .fold(mutableListOf<MutableList<HistoryItem>>()) { acc, item ->
            if (acc.isEmpty() || 
                item.playedAt - acc.last().last().playedAt > 30 * 60 * 1000) {
                acc.add(mutableListOf(item))
            } else {
                acc.last().add(item)
            }
            acc
        }
    
    val avgSessionLength = if (sessions.isNotEmpty()) {
        sessions.map { session -> session.sumOf { it.duration } }.average().toLong()
    } else 0L
    
    return ListeningStats(
        totalListeningTime = totalTime,
        songsPlayed = songsPlayed,
        topGenre = "Pop", // TODO: Calculate from actual data
        averageSessionLength = avgSessionLength
    )
}

private fun formatDuration(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis / (1000 * 60)) % 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}

private fun formatTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}


