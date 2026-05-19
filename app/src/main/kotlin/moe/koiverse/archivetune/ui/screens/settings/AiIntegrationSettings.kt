/*
 * ArchiveTune (2026)
 * Â© Chartreux Westia â€” github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.AiApiKeyKey
import moe.koiverse.archivetune.constants.AiApiValidationStatus
import moe.koiverse.archivetune.constants.AiApiValidationStatusKey
import moe.koiverse.archivetune.constants.AiCustomEndpointKey
import moe.koiverse.archivetune.constants.AiMixCountKey
import moe.koiverse.archivetune.constants.AiProvider
import moe.koiverse.archivetune.constants.AiProviderKey
import moe.koiverse.archivetune.constants.AiUserMixJsonKey
import moe.koiverse.archivetune.constants.TranslatorTargetLangKey
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.EditTextPreference
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.ListPreference
import moe.koiverse.archivetune.ui.component.NumberPickerPreference
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.PreferenceGroup
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.TranslatorLang
import moe.koiverse.archivetune.utils.TranslatorLanguages
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.AiIntegrationSettingsViewModel

@Composable
fun AiIntegrationSettings(
    navController: NavController,
    viewModel: AiIntegrationSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val (provider, setProvider) = rememberEnumPreference(AiProviderKey, AiProvider.NONE)
    val (apiKey, setApiKey) = rememberPreference(AiApiKeyKey, "")
    val (customEndpoint, setCustomEndpoint) = rememberPreference(AiCustomEndpointKey, "")
    val (validationStatus, setValidationStatus) =
        rememberEnumPreference(AiApiValidationStatusKey, AiApiValidationStatus.UNKNOWN)
    val (mixCount, setMixCount) = rememberPreference(AiMixCountKey, 5)
    val (mixJson) = rememberPreference(AiUserMixJsonKey, "")
    val (targetLanguage, setTargetLanguage) = rememberPreference(TranslatorTargetLangKey, "ENGLISH")
    var showApiKeyDialog by rememberSaveable { mutableStateOf(false) }

    val languages by produceState(initialValue = emptyList<TranslatorLang>()) {
        value = withContext(Dispatchers.IO) {
            TranslatorLanguages.load(context)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val hasCustomEndpoint = provider != AiProvider.CUSTOM || customEndpoint.isNotBlank()
    val hasApiConfiguration = provider != AiProvider.NONE && apiKey.isNotBlank() && hasCustomEndpoint
    val canTestApi = hasApiConfiguration && !actionState.isTesting
    val canRebuildMix = hasApiConfiguration &&
        validationStatus != AiApiValidationStatus.FAILED &&
        !actionState.isRebuildingMix
    val hasMix = mixJson.isNotBlank()

    if (showApiKeyDialog) {
        ApiKeyDialog(
            value = apiKey,
            onDismiss = { showApiKeyDialog = false },
            onSave = { value ->
                setApiKey(value.trim())
                setValidationStatus(AiApiValidationStatus.UNKNOWN)
            },
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
            ),
        )

        PreferenceGroup(title = stringResource(R.string.ai_provider_settings)) {
            item {
                ListPreference(
                    title = { Text(stringResource(R.string.ai_provider)) },
                    description = stringResource(R.string.ai_provider_desc),
                    icon = { Icon(painterResource(R.drawable.auto_awesome), null) },
                    selectedValue = provider,
                    values = listOf(
                        AiProvider.CHATGPT,
                        AiProvider.GEMINI,
                        AiProvider.CLAUDE,
                        AiProvider.CUSTOM,
                        AiProvider.NONE,
                    ),
                    valueText = { it.label() },
                    onValueSelected = {
                        setProvider(it)
                        setValidationStatus(AiApiValidationStatus.UNKNOWN)
                    },
                )
            }

            item(visible = provider == AiProvider.CUSTOM) {
                EditTextPreference(
                    title = { Text(stringResource(R.string.ai_custom_endpoint)) },
                    icon = { Icon(painterResource(R.drawable.website), null) },
                    value = customEndpoint,
                    onValueChange = {
                        setCustomEndpoint(it.trim())
                        setValidationStatus(AiApiValidationStatus.UNKNOWN)
                    },
                    isInputValid = { it.startsWith("https://") || it.startsWith("http://") },
                )
            }

            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.ai_api_key)) },
                    description = if (apiKey.isBlank()) {
                        stringResource(R.string.ai_api_key_missing)
                    } else {
                        stringResource(R.string.ai_api_key_configured)
                    },
                    icon = { Icon(painterResource(R.drawable.token), null) },
                    onClick = { showApiKeyDialog = true },
                    isEnabled = provider != AiProvider.NONE,
                )
            }

            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.ai_test_api)) },
                    description = validationStatus.label(),
                    icon = { Icon(painterResource(R.drawable.sync), null) },
                    trailingContent = {
                        if (actionState.isTesting) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    },
                    onClick = viewModel::testApi,
                    isEnabled = canTestApi,
                )
            }
        }

        PreferenceGroup(title = stringResource(R.string.ai_translation)) {
            item {
                ListPreference(
                    title = { Text(stringResource(R.string.ai_translation_target)) },
                    icon = { Icon(painterResource(R.drawable.translate), null) },
                    selectedValue = targetLanguage,
                    values = if (languages.isEmpty()) listOf(targetLanguage) else languages.map { it.code },
                    valueText = { code -> languages.firstOrNull { it.code == code }?.name ?: code },
                    onValueSelected = setTargetLanguage,
                )
            }
        }

        PreferenceGroup(title = stringResource(R.string.ai_user_mix)) {
            item {
                NumberPickerPreference(
                    title = { Text(stringResource(R.string.ai_mix_count)) },
                    icon = { Icon(painterResource(R.drawable.playlist_play), null) },
                    value = mixCount,
                    onValueChange = setMixCount,
                    minValue = 1,
                    maxValue = 10,
                    valueText = { context.getString(R.string.ai_mix_count_value, it) },
                )
            }

            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.ai_rebuild_mix)) },
                    description = stringResource(R.string.ai_rebuild_mix_desc),
                    icon = { Icon(painterResource(R.drawable.auto_awesome), null) },
                    trailingContent = {
                        if (actionState.isRebuildingMix) {
                            CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    },
                    onClick = { viewModel.rebuildMix(mixCount) },
                    isEnabled = canRebuildMix,
                )
            }

            item {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.ai_remove_mix)) },
                    description = stringResource(R.string.ai_remove_mix_desc),
                    icon = { Icon(painterResource(R.drawable.delete), null) },
                    onClick = viewModel::removeMix,
                    isEnabled = hasMix,
                )
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.ai_integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.back_button_desc),
                )
            }
        },
    )
}

@Composable
private fun ApiKeyDialog(
    value: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }

    DefaultDialog(
        onDismiss = onDismiss,
        icon = { Icon(painterResource(R.drawable.token), contentDescription = null) },
        title = { Text(stringResource(R.string.ai_api_key)) },
        buttons = {
            ApiKeyDialogButtons(
                canSave = field.text.isNotBlank(),
                onDismiss = onDismiss,
                onSave = {
                    onSave(field.text)
                    onDismiss()
                },
            )
        },
    ) {
        OutlinedTextField(
            value = field,
            onValueChange = { field = it },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            label = { Text(stringResource(R.string.ai_api_key)) },
        )
    }
}

@Composable
private fun RowScope.ApiKeyDialogButtons(
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
        Text(stringResource(android.R.string.cancel))
    }
    TextButton(
        enabled = canSave,
        onClick = onSave,
        shapes = ButtonDefaults.shapes(),
    ) {
        Text(stringResource(R.string.save))
    }
}

@Composable
private fun AiProvider.label(): String =
    when (this) {
        AiProvider.CHATGPT -> "ChatGPT"
        AiProvider.GEMINI -> "Gemini"
        AiProvider.CLAUDE -> "Claude"
        AiProvider.CUSTOM -> stringResource(R.string.custom)
        AiProvider.NONE -> stringResource(R.string.ai_provider_none)
    }

@Composable
private fun AiApiValidationStatus.label(): String =
    when (this) {
        AiApiValidationStatus.UNKNOWN -> stringResource(R.string.ai_api_status_unknown)
        AiApiValidationStatus.SUCCESS -> stringResource(R.string.ai_api_status_success)
        AiApiValidationStatus.FAILED -> stringResource(R.string.ai_api_status_failed)
    }
