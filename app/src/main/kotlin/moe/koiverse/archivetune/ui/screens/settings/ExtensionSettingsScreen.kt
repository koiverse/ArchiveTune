package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavBackStackEntry
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.di.ExtensionManagerEntryPoint
import moe.koiverse.archivetune.extensions.system.ExtensionManager
import moe.koiverse.archivetune.extensions.system.SettingType
import moe.koiverse.archivetune.extensions.system.ExtensionSettingsStore
import moe.koiverse.archivetune.ui.component.IconButton as M3IconButton
import moe.koiverse.archivetune.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionSettingsScreen(
    navController: NavController,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    backStackEntry: NavBackStackEntry
){
    val context = androidx.compose.ui.platform.LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, ExtensionManagerEntryPoint::class.java)
    val manager: ExtensionManager = entryPoint.extensionManager()
    val id = backStackEntry.arguments?.getString("id") ?: ""
    val ext = manager.installed.value.firstOrNull { it.manifest.id == id }
    if (ext == null) {
        TopAppBar(
            title = { Text("Extension Settings") },
            navigationIcon = {
                M3IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                    Icon(painterResource(R.drawable.arrow_back), null)
                }
            },
            scrollBehavior = scrollBehavior
        )
        return
    }
    val store = remember { ExtensionSettingsStore(context, id) }
    val scope = rememberCoroutineScope()

    Column(Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        ext.manifest.settings.forEach { s ->
            when (s.type) {
                SettingType.toggle -> {
                    var checked by remember { mutableStateOf(store.getBoolean(s.key, s.defaultBoolean ?: false)) }
                    RowItem(
                        title = s.label,
                        trailing = {
                            Switch(checked = checked, onCheckedChange = {
                                checked = it
                                scope.launch {
                                    store.setBoolean(s.key, it)
                                    manager.reload(id)
                                }
                            })
                        }
                    )
                }
                SettingType.slider -> {
                    val def = s.defaultNumber ?: 0
                    val min = s.min ?: 0
                    val max = s.max ?: 100
                    val step = s.step ?: 1
                    var value by remember { mutableStateOf(store.getInt(s.key, def)) }
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(s.label, style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = value.toFloat(),
                            onValueChange = { value = it.toInt() },
                            onValueChangeFinished = {
                                scope.launch {
                                    store.setInt(s.key, value.coerceIn(min, max))
                                    manager.reload(id)
                                }
                            },
                            valueRange = min.toFloat()..max.toFloat(),
                        )
                        Text("$value", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(ext.manifest.name) },
        navigationIcon = {
            M3IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(painterResource(R.drawable.arrow_back), null)
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun RowItem(
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        trailing()
    }
}
