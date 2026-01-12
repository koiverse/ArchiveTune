package moe.koiverse.archivetune.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dagger.hilt.android.EntryPointAccessors
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint
import moe.koiverse.archivetune.extensions.system.ExtensionManager
import moe.koiverse.archivetune.ui.component.IconButton as M3IconButton
import moe.koiverse.archivetune.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    navController: NavController,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager: ExtensionManager = entryPoint.extensionManager()
    val extensions by manager.installed.collectAsState(emptyList())

    var menuExpanded by remember { mutableStateOf(false) }

    val installLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val res = runCatching { managerInstallFromDevice(manager, uri) }
            if (res.isSuccess) {
                Toast.makeText(context, "Extension installed", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Installation failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (extensions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val visible = remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible.value = true }

                    AnimatedVisibility(
                        visible = visible.value,
                        enter = fadeIn(animationSpec = tween(350))
                    ) {
                        Image(
                            painter = painterResource(R.drawable.anime_blank),
                            contentDescription = null,
                            modifier = Modifier.height(140.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.no_extension_installed),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { installLauncher.launch(arrayOf("application/zip")) }
                    ) {
                        Icon(painterResource(R.drawable.add), null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_extension))
                    }
                }
        } else {
        extensions.forEach { ext ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconFile = ext.dir.resolve("icon.png")
                    if (iconFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(iconFile).build(),
                            contentDescription = null,
                            modifier = Modifier.height(40.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.integration),
                            contentDescription = null
                        )
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(ext.manifest.name, style = MaterialTheme.typography.titleMedium)
                        Text("v" + ext.manifest.version, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ext.manifest.allowSettings) {
                        IconButton(onClick = { navController.navigate("settings/extension/${ext.manifest.id}") }) {
                            Icon(painterResource(R.drawable.settings), null)
                        }
                    }
                    Switch(
                        checked = ext.enabled,
                        onCheckedChange = {
                            if (it) manager.enable(ext.manifest.id) else manager.disable(ext.manifest.id)
                        }
                    )
                }
            }
        }
    }
        }
    }


    TopAppBar(
        title = { Text(stringResource(R.string.extensions)) },
        navigationIcon = {
            M3IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(painterResource(R.drawable.arrow_back), null)
            }
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(painterResource(R.drawable.more_vert), null)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.install_from_device)) },
                    onClick = {
                        menuExpanded = false
                        installLauncher.launch(arrayOf("application/zip"))
                    },
                    leadingIcon = { Icon(painterResource(R.drawable.restore), null) }
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

private fun managerInstallFromDevice(manager: ExtensionManager, uri: Uri) {
    manager.installFromZip(uri)
}

