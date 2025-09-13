package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.utils.rememberPreference
import androidx.datastore.preferences.core.booleanPreferencesKey
import moe.koiverse.archivetune.ui.screens.settings.DiscordPresenceManager
import androidx.compose.runtime.collectAsState
import moe.koiverse.archivetune.utils.makeTimeString
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.ui.unit.Dp
import moe.koiverse.archivetune.utils.GlobalLog

// single GlobalLog import above
@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun DebugSettings(
    navController: NavController
) {
    // Developer preferences
    val (showDevDebug, onShowDevDebugChange) = rememberPreference(
        key = booleanPreferencesKey("dev_show_discord_debug"),
        defaultValue = false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Settings") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding: androidx.compose.foundation.layout.PaddingValues ->
        Column(Modifier.padding(innerPadding).padding(16.dp)) {
            PreferenceEntry(
                title = { Text("Show Discord debug UI") },
                description = "Enable debug lines in Discord settings",
                icon = { Icon(painterResource(R.drawable.info), null) },
                trailingContent = {
                    Switch(checked = showDevDebug, onCheckedChange = onShowDevDebugChange)
                }
            )

            if (showDevDebug) {
                // Show manager status lines (observe flows so UI updates)
                val lastStartTs: Long? by DiscordPresenceManager.lastRpcStartTimeFlow.collectAsState(initial = null)
                val lastEndTs: Long? by DiscordPresenceManager.lastRpcEndTimeFlow.collectAsState(initial = null)
                val lastStart: String = lastStartTs?.let { makeTimeString(it) } ?: "-"
                val lastEnd: String = lastEndTs?.let { makeTimeString(it) } ?: "-"

                PreferenceEntry(
                    title = { Text(if (DiscordPresenceManager.isRunning()) "Presence manager: running" else "Presence manager: stopped") },
                    description = "Last RPC start: $lastStart  end: $lastEnd",
                    icon = { Icon(painterResource(R.drawable.info), null) }
                )

                // Log panel with filters, search, and share — Logra-inspired UI
                val allLogs by GlobalLog.logs.collectAsState()
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

                val filterMode = remember { mutableStateOf("discord-only") } // "all" or "discord-only"
                val query = remember { mutableStateOf("") }

                // Filter logs by tag/class or search query
                val filtered = remember(allLogs, filterMode.value, query.value) {
                    allLogs.filter { entry ->
                        val tagMatch = when (filterMode.value) {
                            "discord-only" -> (entry.tag?.contains("DiscordRPC", true) == true) || (entry.tag?.contains("DiscordPresenceManager", true) == true) || entry.message.contains("DiscordPresenceManager") || entry.message.contains("DiscordRPC")
                            else -> true
                        }
                        val q = query.value.trim()
                        val textMatch = q.isEmpty() || entry.message.contains(q, ignoreCase = true) || (entry.tag?.contains(q, ignoreCase = true) == true)
                        tagMatch && textMatch
                    }
                }

                // Lazy list state for auto-scroll
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp))
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Logs", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { filterMode.value = if (filterMode.value == "all") "discord-only" else "all" }) {
                                    Text(if (filterMode.value == "all") "All logs" else "Discord-only")
                                }
                                TextButton(onClick = { GlobalLog.clear() }) { Text("Clear") }
                                TextButton(onClick = {
                                    // Share filtered logs
                                    val sb = StringBuilder()
                                    filtered.forEach { sb.appendLine(GlobalLog.format(it)) }
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, sb.toString())
                                    }
                                    context.startActivity(Intent.createChooser(send, "Share logs"))
                                }) { Text("Share") }
                            }
                        }

                        OutlinedTextField(
                            value = query.value,
                            onValueChange = { query.value = it },
                            label = { Text("Search logs") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Log list
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            itemsIndexed(filtered) { index, entry ->
                                val color = when (entry.level) {
                                    android.util.Log.ERROR -> MaterialTheme.colorScheme.error
                                    android.util.Log.WARN -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                // Single-line header with badge, timestamp and tag
                                val header = buildString {
                                    append("[")
                                    append(android.text.format.DateFormat.format("MM-dd HH:mm:ss", entry.time))
                                    append("] ")
                                    if (!entry.tag.isNullOrBlank()) {
                                        append(entry.tag)
                                    }
                                }

                                // Expand state — track this row's expanded state
                                val (isExpanded, setExpanded) = remember { androidx.compose.runtime.mutableStateOf(false) }

                                Column(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                            // Level badge
                                            Box(
                                                modifier = Modifier
                                                    .background(color = color, shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = when (entry.level) {
                                                        android.util.Log.ERROR -> "E"
                                                        android.util.Log.WARN -> "W"
                                                        android.util.Log.INFO -> "I"
                                                        else -> "D"
                                                    },
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }

                                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(4.dp))

                                            Text(header, style = MaterialTheme.typography.bodyMedium)
                                        }

                                        // Clicking the row copies the entry message and toggles expansion (no separate buttons)
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }

                                    // Message body (collapsed vs expanded)
                                    Text(
                                        text = if (isExpanded) entry.message else entry.message.lines().firstOrNull() ?: "",
                                        color = color,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 8.dp, top = 4.dp)
                                            .clickable {
                                                // copy entry message to clipboard and toggle expansion
                                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(entry.message))
                                                coroutineScope.launch {
                                                    // small toast-style feedback via snackbar is not available here; no-op
                                                }
                                                setExpanded(!isExpanded)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }

                // Auto-scroll to bottom when new logs are added
                LaunchedEffect(filtered.size) {
                    if (filtered.isNotEmpty()) listState.animateScrollToItem(filtered.size - 1)
                }
            }
        }
    }
}
