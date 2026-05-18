/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */



package moe.koiverse.archivetune.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.LocalPreferenceInGroup
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.utils.resize

@Composable
fun HomeModalSideSheet(
    visible: Boolean,
    isAccountLoading: Boolean,
    isAccountLoggedIn: Boolean,
    accountName: String,
    accountEmail: String,
    accountImageUrl: String?,
    hasUpdate: Boolean,
    onDismiss: () -> Unit,
    onAccountClick: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        BackHandler(onBack = onDismiss)
    }

    val sheetItems = remember(hasUpdate) {
        listOf(
            HomeSideSheetItem(
                key = "new_release",
                iconRes = R.drawable.new_release,
                titleRes = R.string.new_releases,
                route = "new_release",
            ),
            HomeSideSheetItem(
                key = "statistics",
                iconRes = R.drawable.stats,
                titleRes = R.string.stats,
                route = "stats",
            ),
            HomeSideSheetItem(
                key = "history",
                iconRes = R.drawable.history,
                titleRes = R.string.history,
                route = "history",
            ),
            HomeSideSheetItem(
                key = "integration",
                iconRes = R.drawable.integration,
                titleRes = R.string.integration,
                route = "settings/integration",
            ),
            HomeSideSheetItem(
                key = "settings",
                iconRes = R.drawable.settings,
                titleRes = R.string.settings,
                route = "settings",
                showBadge = hasUpdate,
            ),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                initialOffsetX = { it },
            ) + fadeIn(),
            exit = slideOutHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                targetOffsetX = { it },
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                val sheetWidth = (maxWidth * 0.88f).coerceAtMost(420.dp)

                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(sheetWidth),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 0.dp,
                        bottomEnd = 0.dp,
                        bottomStart = 16.dp,
                    ),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.End,
                                ),
                            )
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = stringResource(R.string.close),
                                )
                            }
                            Text(
                                text = stringResource(R.string.side_sheet_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }

                        Column(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            HomeSideSheetAccountCard(
                                isLoading = isAccountLoading,
                                isLoggedIn = isAccountLoggedIn,
                                accountName = accountName,
                                accountEmail = accountEmail,
                                accountImageUrl = accountImageUrl,
                                onClick = onAccountClick,
                            )

                            CompositionLocalProvider(LocalPreferenceInGroup provides true) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    sheetItems.forEachIndexed { index, item ->
                                        PreferenceEntry(
                                            shape = homeSideSheetItemShape(index, sheetItems.size),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            ),
                                            title = {
                                                Text(
                                                    text = stringResource(item.titleRes),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            },
                                            icon = {
                                                if (item.showBadge) {
                                                    BadgedBox(
                                                        badge = { Badge() },
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(item.iconRes),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        painter = painterResource(item.iconRes),
                                                        contentDescription = null,
                                                    )
                                                }
                                            },
                                            trailingContent = {
                                                Icon(
                                                    painter = painterResource(R.drawable.navigate_next),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            },
                                            onClick = { onNavigate(item.route) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeSideSheetAccountCard(
    isLoading: Boolean,
    isLoggedIn: Boolean,
    accountName: String,
    accountEmail: String,
    accountImageUrl: String?,
    onClick: () -> Unit,
) {
    val title = when {
        isLoading -> stringResource(R.string.loading)
        isLoggedIn -> accountName.ifBlank { stringResource(R.string.account) }
        else -> stringResource(R.string.login)
    }
    val subtitle = when {
        isLoggedIn && accountEmail.isNotBlank() -> accountEmail
        isLoggedIn -> accountName.ifBlank { stringResource(R.string.account) }
        else -> stringResource(R.string.side_sheet_account_signed_out_subtitle)
    }
    val avatarUrl = remember(isLoggedIn, accountImageUrl) {
        if (isLoggedIn) accountImageUrl?.resize(160, 160) else null
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 118.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading -> {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    avatarUrl != null -> {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            placeholder = painterResource(R.drawable.person),
                            error = painterResource(R.drawable.person),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        Icon(
                            painter = painterResource(
                                if (isLoggedIn) R.drawable.account else R.drawable.login,
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun homeSideSheetItemShape(index: Int, count: Int): Shape {
    val large = 28.dp
    val small = 6.dp
    return when {
        count <= 1 -> RoundedCornerShape(large)
        index == 0 -> RoundedCornerShape(
            topStart = large,
            topEnd = large,
            bottomEnd = small,
            bottomStart = small,
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = small,
            topEnd = small,
            bottomEnd = large,
            bottomStart = large,
        )
        else -> RoundedCornerShape(small)
    }
}

@Immutable
private data class HomeSideSheetItem(
    val key: String,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    val route: String,
    val showBadge: Boolean = false,
)
