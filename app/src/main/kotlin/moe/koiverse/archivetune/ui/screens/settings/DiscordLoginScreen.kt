/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.discord.DiscordAuthCoordinator
import moe.koiverse.archivetune.discord.DiscordOAuthRepository
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var authorizationSession by remember {
        mutableStateOf(DiscordOAuthRepository.createAuthorizationSession())
    }
    var isWaitingForRedirect by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    fun launchAuthorization() {
        errorMessage = null
        isWaitingForRedirect = true
        runCatching {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, authorizationSession.authorizationUri)
        }.recoverCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, authorizationSession.authorizationUri),
            )
        }.onFailure {
            isWaitingForRedirect = false
            errorMessage = it.message
        }
    }

    LaunchedEffect(authorizationSession) {
        DiscordAuthCoordinator.redirects.collectLatest { redirect ->
            if (redirect.getQueryParameter("state") != authorizationSession.state) {
                return@collectLatest
            }
            isWaitingForRedirect = true
            DiscordOAuthRepository.completeAuthorization(
                context = context,
                session = authorizationSession,
                redirect = redirect,
            ).onSuccess {
                isWaitingForRedirect = false
                navController.navigateUp()
            }.onFailure {
                isWaitingForRedirect = false
                errorMessage = it.message
                authorizationSession = DiscordOAuthRepository.createAuthorizationSession()
            }
        }
    }

    BackHandler(enabled = isWaitingForRedirect) {
        isWaitingForRedirect = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_login)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.discord),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.discord_login_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            errorMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                enabled = !isWaitingForRedirect,
                onClick = {
                    authorizationSession = DiscordOAuthRepository.createAuthorizationSession()
                    scope.launch { launchAuthorization() }
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.discord_open_authorization))
            }

            if (isWaitingForRedirect) {
                Spacer(Modifier.height(24.dp))
                LinearProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.discord_waiting_for_authorization),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
