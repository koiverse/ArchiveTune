/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */


@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.AccountEmailKey
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.Updater
import moe.koiverse.archivetune.utils.rememberPreference
import moe.koiverse.archivetune.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val listState = rememberLazyListState()
    val viewModel: HomeViewModel = hiltViewModel()
    val isAccountLoading by viewModel.isAccountLoading.collectAsStateWithLifecycle()
    val isAccountLoggedIn by viewModel.isAccountLoggedIn.collectAsStateWithLifecycle()
    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val (accountEmail, _) = rememberPreference(AccountEmailKey, "")

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    var isStorageGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isNotificationGranted by remember {
        mutableStateOf(
            notificationPermission == null ||
                ContextCompat.checkSelfPermission(context, notificationPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        isStorageGranted = result[storagePermission] == true || isStorageGranted
        if (notificationPermission != null) {
            isNotificationGranted = result[notificationPermission] == true || isNotificationGranted
        }
    }

    val shouldShowPermissionHint = !isStorageGranted || !isNotificationGranted
    val hasUpdate = !Updater.isSameVersion(latestVersionName, BuildConfig.VERSION_NAME)
    var isUpdateDismissed by remember { mutableStateOf(false) }
    val settingsGroups = buildSettingsGroups(navController, isAndroid12OrLater, hasUpdate, context)

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 32.dp,
            ),
        ) {
            if (hasUpdate && !isUpdateDismissed) {
                item(key = "update") {
                    SettingsUpdateBanner(
                        latestVersion = latestVersionName,
                        onClick = { navController.navigate("settings/update") },
                        onDismiss = { isUpdateDismissed = true },
                        modifier = Modifier
                            .padding(horizontal = SettingsDimensions.ScreenHorizontalPadding)
                            .padding(top = 8.dp, bottom = SettingsDimensions.SectionSpacing),
                    )
                }
            }

            item(key = "profile") {
                SettingsProfileHeader(
                    state = SettingsProfileState(
                        isLoading = isAccountLoading,
                        isLoggedIn = isAccountLoggedIn,
                        accountName = accountName,
                        accountEmail = accountEmail,
                        accountImageUrl = if (isAccountLoggedIn) accountImageUrl else null,
                    ),
                    onClick = { navController.navigate("settings/account") },
                )
            }

            if (shouldShowPermissionHint) {
                item(key = "permission_header") {
                    SettingsSectionLabel(
                        text = stringResource(R.string.permissions_title),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                item(key = "permission") {
                    SettingsPermissionBanner(
                        onRequestPermission = {
                            val toRequest = buildList {
                                if (!isStorageGranted) add(storagePermission)
                                if (!isNotificationGranted && notificationPermission != null) {
                                    add(notificationPermission)
                                }
                            }
                            if (toRequest.isNotEmpty()) {
                                permissionLauncher.launch(toRequest.toTypedArray())
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = SettingsDimensions.ScreenHorizontalPadding)
                            .padding(bottom = 4.dp),
                    )
                }
            }

            settingsGroups.forEach { group ->
                group.items.forEach { settingsItem ->
                    item(key = settingsItem.title) {
                        SettingsFlatItem(item = settingsItem)
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

