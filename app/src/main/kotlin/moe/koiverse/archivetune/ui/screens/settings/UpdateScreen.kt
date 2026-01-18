package moe.koiverse.archivetune.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.EnableUpdateNotificationKey
import moe.koiverse.archivetune.constants.UpdateChannel
import moe.koiverse.archivetune.constants.UpdateChannelKey
import moe.koiverse.archivetune.ui.component.EnumListPreference
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.PreferenceGroupTitle
import moe.koiverse.archivetune.ui.component.SwitchPreference
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.GitCommit
import moe.koiverse.archivetune.utils.UpdateNotificationManager
import moe.koiverse.archivetune.utils.Updater
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    val (enableUpdateNotification, onEnableUpdateNotificationChange) = rememberPreference(
        EnableUpdateNotificationKey,
        defaultValue = false
    )
    val (updateChannel, onUpdateChannelChange) = rememberEnumPreference(
        UpdateChannelKey,
        defaultValue = UpdateChannel.STABLE
    )

    var commits by remember { mutableStateOf<List<GitCommit>>(emptyList()) }
    var isLoadingCommits by remember { mutableStateOf(true) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var isExpanded by remember { mutableStateOf(true) }
    var showNightlyInstallConfirm by remember { mutableStateOf(false) }
    var showNightlyInstallProgress by remember { mutableStateOf(false) }
    var nightlyInstallProgress by remember { mutableStateOf<Float?>(null) }
    var nightlyInstallStage by remember { mutableStateOf("") }
    var nightlyInstallError by remember { mutableStateOf<String?>(null) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            onEnableUpdateNotificationChange(true)
            UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            Updater.getLatestVersionName().onSuccess {
                latestVersion = it
            }
            Updater.getCommitHistory(30).onSuccess {
                commits = it
            }.onFailure {
                commits = emptyList()
            }
            isLoadingCommits = false
        }
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.updates)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.update),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.current_version),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            latestVersion?.let { latest ->
                                if (latest != BuildConfig.VERSION_NAME) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.latest_version_format, latest),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                PreferenceGroupTitle(title = stringResource(R.string.notification_settings))
            }

            item {
                SwitchPreference(
                    title = { Text(stringResource(R.string.enable_update_notification)) },
                    description = stringResource(R.string.enable_update_notification_desc),
                    icon = { Icon(painterResource(R.drawable.notifications_unread), null) },
                    checked = enableUpdateNotification,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onEnableUpdateNotificationChange(true)
                                UpdateNotificationManager.schedulePeriodicUpdateCheck(context)
                            }
                        } else {
                            onEnableUpdateNotificationChange(false)
                            UpdateNotificationManager.cancelPeriodicUpdateCheck(context)
                        }
                    }
                )
            }

            item {
                EnumListPreference(
                    title = { Text(stringResource(R.string.update_channel)) },
                    icon = { Icon(painterResource(R.drawable.tune), null) },
                    selectedValue = updateChannel,
                    valueText = { channel ->
                        when (channel) {
                            UpdateChannel.STABLE -> stringResource(R.string.channel_stable)
                            UpdateChannel.NIGHTLY -> stringResource(R.string.channel_nightly)
                        }
                    },
                    onValueSelected = onUpdateChannelChange
                )
            }

            item {
                AnimatedVisibility(visible = updateChannel == UpdateChannel.NIGHTLY) {
                    val latestCommitHash = commits.firstOrNull()?.sha ?: "—"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Nightly Builds",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Latest features and fixes from the development branch. May contain experimental features and occasional bugs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = latestCommitHash,
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showNightlyInstallConfirm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Install")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                PreferenceGroupTitle(title = stringResource(R.string.commit_history))
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.history),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.recent_commits),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.expand_more),
                                contentDescription = null,
                                modifier = Modifier.rotate(rotationAngle)
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                if (isLoadingCommits) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isExpanded && !isLoadingCommits) {
                items(commits) { commit ->
                    CommitItem(
                        commit = commit,
                        onClick = { uriHandler.openUri(commit.url) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showNightlyInstallConfirm) {
        AlertDialog(
            onDismissRequest = { showNightlyInstallConfirm = false },
            title = { Text("Install Nightly Build") },
            text = {
                Text(
                    "Download and install the latest dev build now? This may contain experimental changes."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNightlyInstallConfirm = false
                        nightlyInstallError = null
                        showNightlyInstallProgress = true
                        nightlyInstallStage = "Downloading"
                        nightlyInstallProgress = 0f

                        coroutineScope.launch {
                            runCatching {
                                val (zipFile, apkFile) = withContext(Dispatchers.IO) {
                                    val artifactName =
                                        if (BuildConfig.ARCHITECTURE == "universal") {
                                            "app-universal-release"
                                        } else {
                                            "app-${BuildConfig.ARCHITECTURE}-release"
                                        }

                                    val downloadUrl =
                                        "https://nightly.link/koiverse/ArchiveTune/workflows/build.yml/dev/$artifactName.zip"

                                    val outputZip = File(context.cacheDir, "nightly_build.zip")
                                    val outputApk = File(context.cacheDir, "nightly_build.apk")

                                    downloadToFileWithProgress(
                                        url = downloadUrl,
                                        outputFile = outputZip,
                                        onProgress = { progress ->
                                            coroutineScope.launch {
                                                nightlyInstallProgress = progress
                                            }
                                        }
                                    )

                                    coroutineScope.launch {
                                        nightlyInstallStage = "Unzipping"
                                        nightlyInstallProgress = null
                                    }

                                    val extractedApk = extractFirstApkFromZip(outputZip, outputApk)
                                    outputZip to extractedApk
                                }

                                withContext(Dispatchers.Main) {
                                    showNightlyInstallProgress = false
                                    launchApkInstall(context, apkFile)
                                }

                                withContext(Dispatchers.IO) {
                                    zipFile.delete()
                                }
                            }.onFailure { e ->
                                nightlyInstallError = e.message ?: "Failed to install nightly build"
                                showNightlyInstallProgress = false
                            }
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNightlyInstallConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNightlyInstallProgress) {
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("Installing") },
            text = {
                Column {
                    Text(nightlyInstallStage)
                    Spacer(modifier = Modifier.height(12.dp))
                    val value = nightlyInstallProgress
                    if (value == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(value * 100).toInt()}%")
                    }
                }
            },
            confirmButton = {}
        )
    }

    nightlyInstallError?.let { error ->
        AlertDialog(
            onDismissRequest = { nightlyInstallError = null },
            title = { Text("Nightly Install Failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { nightlyInstallError = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun CommitItem(
    commit: GitCommit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commit.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = commit.sha,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = commit.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (commit.date.isNotEmpty()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = formatCommitDate(commit.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

private fun downloadToFileWithProgress(
    url: String,
    outputFile: File,
    onProgress: (Float?) -> Unit,
) {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = 15_000
        readTimeout = 30_000
        requestMethod = "GET"
    }

    connection.connect()
    if (connection.responseCode !in 200..299) {
        throw IllegalStateException("HTTP ${connection.responseCode}")
    }

    val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
    outputFile.parentFile?.mkdirs()

    var downloaded = 0L
    var lastEmitTime = 0L

    connection.inputStream.use { input ->
        FileOutputStream(outputFile).use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                downloaded += read.toLong()

                val now = System.currentTimeMillis()
                if (now - lastEmitTime >= 80L) {
                    val progress =
                        if (totalBytes == null) null else (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    onProgress(progress)
                    lastEmitTime = now
                }
            }
            output.flush()
        }
    }

    onProgress(1f)
    connection.disconnect()
}

private fun extractFirstApkFromZip(zipFile: File, outputApk: File): File {
    ZipInputStream(FileInputStream(zipFile)).use { zis ->
        while (true) {
            val entry = zis.nextEntry ?: break
            val name = entry.name
            if (!entry.isDirectory && name.endsWith(".apk", ignoreCase = true)) {
                outputApk.parentFile?.mkdirs()
                FileOutputStream(outputApk).use { out ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = zis.read(buffer)
                        if (read <= 0) break
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
                return outputApk
            }
        }
    }
    throw IllegalStateException("No APK found in downloaded zip")
}

private fun launchApkInstall(context: android.content.Context, apkFile: File) {
    val authority = "${context.packageName}.FileProvider"
    val apkUri = FileProvider.getUriForFile(context, authority, apkFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

private fun formatCommitDate(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(isoDate)
        val outputFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        outputFormat.format(date!!)
    } catch (e: Exception) {
        isoDate.take(10)
    }
}
