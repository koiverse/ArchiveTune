package moe.koiverse.archivetune.ui.screens.settings

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordTokenLoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var tokenInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var discordToken by rememberPreference(DiscordTokenKey, "")
    var discordUsername by rememberPreference(DiscordUsernameKey, "")
    var discordName by rememberPreference(DiscordNameKey, "")

    fun validateAndLogin() {
        if (tokenInput.isBlank()) {
            errorMessage = context.getString(R.string.discord_token_cannot_be_empty)
            return
        }

        isValidating = true
        errorMessage = null
        keyboardController?.hide()

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    KizzyRPC.getUserInfo(tokenInput.trim())
                }

                result.onSuccess { userInfo ->
                    discordToken = tokenInput.trim()
                    discordUsername = userInfo.username
                    discordName = userInfo.name
                    isValidating = false
                    navController.navigateUp()
                }.onFailure {
                    errorMessage = context.getString(R.string.discord_token_validation_failed)
                    isValidating = false
                }
            } catch (e: Exception) {
                Log.e("DiscordTokenLogin", "Token validation failed")
                errorMessage = context.getString(R.string.discord_network_error_retry)
                isValidating = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.discord_token_login_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.discord_token_info),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start
                    )
                }
            }

            OutlinedTextField(
                value = tokenInput,
                onValueChange = {
                    tokenInput = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.discord_token_input_hint)) },
                placeholder = { Text(stringResource(R.string.discord_token_input_hint)) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    M3IconButton(
                        onClick = { passwordVisible = !passwordVisible }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (passwordVisible) R.drawable.visibility_off else R.drawable.visibility
                            ),
                            contentDescription = if (passwordVisible) stringResource(R.string.discord_hide_token) else stringResource(R.string.discord_show_token)
                        )
                    }
                },
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it) } },
                singleLine = false,
                maxLines = 3,
                enabled = !isValidating,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { validateAndLogin() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { validateAndLogin() },
                enabled = !isValidating && tokenInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isValidating) stringResource(R.string.discord_validating) else stringResource(R.string.action_login))
            }
        }
    }
}
