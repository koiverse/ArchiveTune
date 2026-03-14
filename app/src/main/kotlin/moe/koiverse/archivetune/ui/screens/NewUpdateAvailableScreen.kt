/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */


package moe.koiverse.archivetune.ui.screens

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.content.ActivityNotFoundException
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.Updater
import moe.koiverse.archivetune.utils.ReleaseInfo
import androidx.compose.ui.platform.LocalUriHandler
import com.mikepenz.markdown.m3.Markdown
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import moe.koiverse.archivetune.viewmodels.NewUpdateAvailableViewModel

@Composable
fun NewUpdateAvailableScreen(
    navController: NavController,
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val viewModel: NewUpdateAvailableViewModel = hiltViewModel()

    val latestVersion by viewModel.latestVersion.collectAsState(null)
    val downloadProgress by viewModel.downloadProgress.collectAsState(-1f)
    val downloadState by viewModel.downloadState.collectAsState(DownloadManager.STATUS_PENDING)
    val installIntent by viewModel.installIntent.collectAsState(null)
    val isDownloading by viewModel.isDownloading.collectAsState(false)
    val error by viewModel.error.collectAsState(null)

    val context = LocalContext.current
    val downloadReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    viewModel.onDownloadComplete(intent)
                }
            }
        }
    }

    // Observe download done event
    LaunchedEffect(Unit) {
        viewModel.onDownloadDone.collect { intent ->
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.startDownload()
        } else {
            // User denied permission, open browser to download
            latestVersion?.let {
                uriHandler.openUri(Updater.getLatestDownloadUrl(it))
            }
        }
    }

    DisposableEffect(Unit) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_EXPORTED
        } else {
            0
        }
        context.registerReceiver(
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            flags
        )
        onDispose {
            context.unregisterReceiver(downloadReceiver)
        }
    }

    LaunchedEffect(installIntent) {
        installIntent?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (context.packageManager.canRequestPackageInstalls()) {
                    try {
                        context.startActivity(it)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
            } else {
                try {
                    context.startActivity(it)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.update),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.new_update_available),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            latestVersion?.let {
                Text(
                    text = stringResource(R.string.latest_version_format, it.name),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.current_version),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            latestVersion?.body?.let { notes ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                ) {
                    Markdown(
                        content = notes,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            if (isDownloading) {
                LinearProgressIndicator(
                    progress = downloadProgress,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.download_progress, (downloadProgress * 100).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.weight(1f),
                    enabled = !isDownloading,
                ) {
                    Text(text = stringResource(R.string.cancel))
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        when {
                            installIntent != null -> {
                                // If already have file APK, request install unknown apps permission and open install
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    if (context.packageManager.canRequestPackageInstalls()) {
                                        installIntent?.let {
                                            try {
                                                context.startActivity(it)
                                            } catch (e: ActivityNotFoundException) {
                                                e.printStackTrace()
                                            }
                                    }
                                }
                            } else {
                                installIntent?.let {
                                    try {
                                        context.startActivity(it)
                                        } catch (e: ActivityNotFoundException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }

                            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                                // Request write external storage permission download for Android < 10
                                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }

                            else -> {
                                // No need write external storage permission for Android >= 10
                                viewModel.startDownload()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = latestVersion != null && !isDownloading,
                ) {
                    val buttonText = when {
                        installIntent != null -> stringResource(R.string.install)
                        isDownloading -> stringResource(R.string.downloading)
                        else -> stringResource(R.string.update_text)
                    }
                    Text(text = buttonText)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
