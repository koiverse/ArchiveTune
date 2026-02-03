package moe.koiverse.archivetune.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.my.kizzy.rpc.KizzyRPC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.DiscordNameKey
import moe.koiverse.archivetune.constants.DiscordTokenKey
import moe.koiverse.archivetune.constants.DiscordUsernameKey
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.TextFieldDialog
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordTokenViewScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var discordToken by rememberPreference(DiscordTokenKey, "")
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")

    var tokenVisible by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val maskedToken = remember(discordToken) {
        "•".repeat(minOf(discordToken.length, 40))
    }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Discord Token", discordToken)
        clipboard.setPrimaryClip(clip)
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(R.string.discord_token_copied))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.discord_token_view_title)) },
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
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Token",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        M3IconButton(
                            onClick = { tokenVisible = !tokenVisible }
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (tokenVisible) R.drawable.visibility_off else R.drawable.visibility
                                ),
                                contentDescription = if (tokenVisible) "Hide token" else "Show token"
                            )
                        }
                    }

                    if (tokenVisible) {
                        SelectionContainer {
                            Text(
                                text = discordToken,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Text(
                            text = maskedToken,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { copyToClipboard() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy")
                        }

                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }
                }
            }

            PreferenceEntry(
                title = { Text(stringResource(R.string.discord_delete_token)) },
                icon = { Icon(painterResource(R.drawable.delete), null, tint = MaterialTheme.colorScheme.error) },
                onClick = { showDeleteConfirmDialog = true }
            )
        }
    }

    if (showEditDialog) {
        var isValidating by remember { mutableStateOf(false) }
        var validationError by remember { mutableStateOf<String?>(null) }

        TextFieldDialog(
            title = { Text("Edit Discord Token") },
            initialTextFieldValue = TextFieldValue(discordToken),
            singleLine = false,
            maxLines = 3,
            placeholder = { Text(stringResource(R.string.discord_token_input_hint)) },
            onDone = { newToken ->
                isValidating = true
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            KizzyRPC.getUserInfo(newToken.trim())
                        }

                        result.onSuccess { userInfo ->
                            discordToken = newToken.trim()
                            discordUsername = userInfo.username
                            discordName = userInfo.name
                            showEditDialog = false
                            isValidating = false
                            snackbarHostState.showSnackbar("Token updated successfully")
                        }.onFailure {
                            validationError = "Invalid token"
                            isValidating = false
                        }
                    } catch (e: Exception) {
                        Log.e("DiscordTokenView", "Token validation failed", e)
                        validationError = "Network error: ${e.message ?: "Unknown error"}"
                        isValidating = false
                    }
                }
            },
            onDismiss = { 
                if (!isValidating) {
                    showEditDialog = false
                    validationError = null
                }
            },
            extraContent = {
                if (isValidating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Validating...")
                    }
                }
                validationError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.discord_delete_token)) },
            text = { Text(stringResource(R.string.discord_delete_token_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        discordToken = ""
                        discordUsername = ""
                        discordName = ""
                        showDeleteConfirmDialog = false
                        navController.navigateUp()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
