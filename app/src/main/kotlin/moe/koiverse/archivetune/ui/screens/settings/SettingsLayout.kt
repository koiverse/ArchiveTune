/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass
import moe.koiverse.archivetune.LocalAnimationsDisabled
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets

enum class SettingsLayoutMode {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

data class SettingsProfileState(
    val isLoading: Boolean,
    val isLoggedIn: Boolean,
    val accountName: String,
    val accountEmail: String,
    val accountImageUrl: String?,
)

@Composable
fun resolveLayoutMode(): SettingsLayoutMode {
    val windowInfo = currentWindowAdaptiveInfo().windowSizeClass
    return when {
        windowInfo.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) ->
            SettingsLayoutMode.EXPANDED
        windowInfo.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) ->
            SettingsLayoutMode.MEDIUM
        else ->
            SettingsLayoutMode.COMPACT
    }
}

data class SettingsContentState(
    val profileHeader: SettingsProfileState,
    val quickActions: List<SettingsQuickAction>,
    val integrations: List<SettingsIntegrationAction>,
    val groups: List<SettingsGroup>,
    val internalGroup: SettingsGroup?,
    val showPermissionBanner: Boolean,
    val showUpdateBanner: Boolean,
    val latestVersion: String,
    val isSearchActive: Boolean,
    val hasSearchResults: Boolean,
    val onProfileHeaderClick: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onUpdateClick: () -> Unit,
)

@Composable
fun AdaptiveSettingsLayout(
    state: SettingsContentState,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    topPadding: Dp = 0.dp,
) {
    val layoutMode = resolveLayoutMode()
    val animationsDisabled = LocalAnimationsDisabled.current

    var heroVisible by remember { mutableStateOf(false) }
    var bannerVisible by remember { mutableStateOf(false) }
    var quickActionsVisible by remember { mutableStateOf(false) }
    var integrationsVisible by remember { mutableStateOf(false) }
    var categoriesVisible by remember { mutableStateOf(false) }

    LaunchedEffect(animationsDisabled) {
        if (animationsDisabled) {
            heroVisible = true
            bannerVisible = true
            quickActionsVisible = true
            integrationsVisible = true
            categoriesVisible = true
            return@LaunchedEffect
        }

        val anim = Animatable(0f)
        anim.animateTo(1f, tween(50))
        heroVisible = true
        anim.animateTo(1f, tween(60))
        bannerVisible = true
        anim.animateTo(1f, tween(60))
        quickActionsVisible = true
        anim.animateTo(1f, tween(70))
        integrationsVisible = true
        anim.animateTo(1f, tween(70))
        categoriesVisible = true
    }

    val quickActionColumns = when (layoutMode) {
        SettingsLayoutMode.COMPACT -> SettingsDimensions.CompactColumns
        SettingsLayoutMode.MEDIUM -> SettingsDimensions.MediumColumns
        SettingsLayoutMode.EXPANDED -> SettingsDimensions.ExpandedColumns
    }

    when (layoutMode) {
        SettingsLayoutMode.COMPACT -> {
            CompactSettingsLayout(
                state = state,
                listState = listState,
                quickActionColumns = quickActionColumns,
                heroVisible = heroVisible,
                bannerVisible = bannerVisible,
                quickActionsVisible = quickActionsVisible,
                integrationsVisible = integrationsVisible,
                categoriesVisible = categoriesVisible,
                topPadding = topPadding,
                modifier = modifier,
            )
        }
        SettingsLayoutMode.MEDIUM -> {
            MediumSettingsLayout(
                state = state,
                quickActionColumns = quickActionColumns,
                heroVisible = heroVisible,
                bannerVisible = bannerVisible,
                quickActionsVisible = quickActionsVisible,
                integrationsVisible = integrationsVisible,
                categoriesVisible = categoriesVisible,
                topPadding = topPadding,
                modifier = modifier,
            )
        }
        SettingsLayoutMode.EXPANDED -> {
            ExpandedSettingsLayout(
                state = state,
                quickActionColumns = quickActionColumns,
                heroVisible = heroVisible,
                bannerVisible = bannerVisible,
                quickActionsVisible = quickActionsVisible,
                integrationsVisible = integrationsVisible,
                categoriesVisible = categoriesVisible,
                topPadding = topPadding,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CompactSettingsLayout(
    state: SettingsContentState,
    listState: LazyListState,
    quickActionColumns: Int,
    heroVisible: Boolean,
    bannerVisible: Boolean,
    quickActionsVisible: Boolean,
    integrationsVisible: Boolean,
    categoriesVisible: Boolean,
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            ),
        contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
    ) {
        item(key = "hero") {
            AnimatedVisibility(
                visible = heroVisible,
                enter = fadeIn(SettingsAnimations.entranceSpring()) +
                    slideInVertically(
                        initialOffsetY = { -it / 5 },
                        animationSpec = SettingsAnimations.entranceSpring(),
                    ),
            ) {
                SettingsProfileHeader(
                    state = state.profileHeader,
                    onClick = state.onProfileHeaderClick,
                    modifier = Modifier
                        .padding(horizontal = pad)
                        .padding(top = 4.dp, bottom = spacing),
                )
            }
        }

        if (!state.isSearchActive) {
            item(key = "permission") {
                AnimatedVisibility(
                    visible = bannerVisible && state.showPermissionBanner,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) +
                        expandVertically(SettingsAnimations.entranceSpring()),
                    exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                ) {
                    SettingsPermissionBanner(
                        onRequestPermission = state.onRequestPermission,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(bottom = spacing),
                    )
                }
            }

            item(key = "update") {
                AnimatedVisibility(
                    visible = bannerVisible && state.showUpdateBanner,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) +
                        expandVertically(SettingsAnimations.entranceSpring()),
                    exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                ) {
                    SettingsUpdateBanner(
                        latestVersion = state.latestVersion,
                        onClick = state.onUpdateClick,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(bottom = spacing),
                    )
                }
            }
        }

        if (state.quickActions.isNotEmpty()) {
            item(key = "quickActions") {
                AnimatedVisibility(
                    visible = quickActionsVisible,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) +
                        slideInVertically(
                            initialOffsetY = { it / 6 },
                            animationSpec = SettingsAnimations.entranceSpring(),
                        ),
                ) {
                    SettingsQuickActionsSection(
                        actions = state.quickActions,
                        columns = quickActionColumns,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(bottom = spacing),
                    )
                }
            }
        }

        if (state.integrations.isNotEmpty()) {
            item(key = "integrations") {
                AnimatedVisibility(
                    visible = integrationsVisible,
                    enter = fadeIn(SettingsAnimations.entranceSpring()) +
                        slideInVertically(
                            initialOffsetY = { it / 6 },
                            animationSpec = SettingsAnimations.entranceSpring(),
                        ),
                ) {
                    SettingsIntegrationsSection(
                        integrations = state.integrations,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(bottom = spacing),
                    )
                }
            }
        }

        if (state.isSearchActive && !state.hasSearchResults) {
            item(key = "empty") {
                Spacer(modifier = Modifier.height(24.dp))
                SettingsSearchEmpty(
                    modifier = Modifier.padding(horizontal = pad),
                )
            }
        } else {
            if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) {
                item(key = "internalSearchResults") {
                    SettingsGroupCard(
                        group = state.internalGroup,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(bottom = spacing),
                    )
                }
            }

            items(
                count = state.groups.size,
                key = { state.groups[it].title },
            ) { index ->
                val group = state.groups[index]
                AnimatedVisibility(
                    visible = categoriesVisible,
                    enter = fadeIn(
                        SettingsAnimations.staggerTween(index)
                    ) + slideInVertically(
                        initialOffsetY = { it / 5 },
                        animationSpec = SettingsAnimations.staggerTween(index),
                    ),
                ) {
                    SettingsGroupCard(
                        group = group,
                        modifier = Modifier
                            .padding(horizontal = pad)
                            .padding(bottom = spacing),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediumSettingsLayout(
    state: SettingsContentState,
    quickActionColumns: Int,
    heroVisible: Boolean,
    bannerVisible: Boolean,
    quickActionsVisible: Boolean,
    integrationsVisible: Boolean,
    categoriesVisible: Boolean,
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    Row(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .padding(horizontal = pad),
        horizontalArrangement = Arrangement.spacedBy(pad),
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(SettingsDimensions.MediumPaneLeftWeight)
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
        ) {
            item(key = "hero") {
                AnimatedVisibility(
                    visible = heroVisible,
                    enter = fadeIn(SettingsAnimations.entranceSpring()),
                ) {
                    SettingsProfileHeader(
                        state = state.profileHeader,
                        onClick = state.onProfileHeaderClick,
                        modifier = Modifier.padding(top = 4.dp, bottom = spacing),
                    )
                }
            }

            if (!state.isSearchActive) {
                item(key = "permission") {
                    AnimatedVisibility(
                        visible = bannerVisible && state.showPermissionBanner,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) +
                            expandVertically(SettingsAnimations.entranceSpring()),
                        exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                    ) {
                        SettingsPermissionBanner(
                            onRequestPermission = state.onRequestPermission,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }

                item(key = "update") {
                    AnimatedVisibility(
                        visible = bannerVisible && state.showUpdateBanner,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) +
                            expandVertically(SettingsAnimations.entranceSpring()),
                        exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                    ) {
                        SettingsUpdateBanner(
                            latestVersion = state.latestVersion,
                            onClick = state.onUpdateClick,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }

            if (state.quickActions.isNotEmpty()) {
                item(key = "quickActions") {
                    AnimatedVisibility(
                        visible = quickActionsVisible,
                        enter = fadeIn(SettingsAnimations.entranceSpring()),
                    ) {
                        SettingsQuickActionsSection(
                            actions = state.quickActions,
                            columns = 2,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }

            if (state.integrations.isNotEmpty()) {
                item(key = "integrations") {
                    AnimatedVisibility(
                        visible = integrationsVisible,
                        enter = fadeIn(SettingsAnimations.entranceSpring()),
                    ) {
                        SettingsIntegrationsSection(
                            integrations = state.integrations,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(SettingsDimensions.MediumPaneRightWeight)
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
        ) {
            if (state.isSearchActive && !state.hasSearchResults) {
                item(key = "empty") {
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSearchEmpty()
                }
            } else {
                if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) {
                    item(key = "internalSearchResults") {
                        SettingsGroupCard(
                            group = state.internalGroup,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }

                items(
                    count = state.groups.size,
                    key = { state.groups[it].title },
                ) { index ->
                    AnimatedVisibility(
                        visible = categoriesVisible,
                        enter = fadeIn(
                            SettingsAnimations.staggerTween(index)
                        ) + slideInVertically(
                            initialOffsetY = { it / 5 },
                            animationSpec = SettingsAnimations.staggerTween(index),
                        ),
                    ) {
                        SettingsGroupCard(
                            group = state.groups[index],
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedSettingsLayout(
    state: SettingsContentState,
    quickActionColumns: Int,
    heroVisible: Boolean,
    bannerVisible: Boolean,
    quickActionsVisible: Boolean,
    integrationsVisible: Boolean,
    categoriesVisible: Boolean,
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val pad = SettingsDimensions.ScreenHorizontalPadding
    val spacing = SettingsDimensions.SectionSpacing

    Row(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .padding(horizontal = pad),
        horizontalArrangement = Arrangement.spacedBy(pad),
    ) {
        LazyColumn(
            modifier = Modifier
                .width(SettingsDimensions.ExpandedListPaneWidth)
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
        ) {
            item(key = "hero") {
                AnimatedVisibility(
                    visible = heroVisible,
                    enter = fadeIn(SettingsAnimations.entranceSpring()),
                ) {
                    SettingsProfileHeader(
                        state = state.profileHeader,
                        onClick = state.onProfileHeaderClick,
                        modifier = Modifier.padding(top = 4.dp, bottom = spacing),
                    )
                }
            }

            if (!state.isSearchActive) {
                item(key = "permission") {
                    AnimatedVisibility(
                        visible = bannerVisible && state.showPermissionBanner,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) +
                            expandVertically(SettingsAnimations.entranceSpring()),
                        exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                    ) {
                        SettingsPermissionBanner(
                            onRequestPermission = state.onRequestPermission,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }

                item(key = "update") {
                    AnimatedVisibility(
                        visible = bannerVisible && state.showUpdateBanner,
                        enter = fadeIn(SettingsAnimations.entranceSpring()) +
                            expandVertically(SettingsAnimations.entranceSpring()),
                        exit = fadeOut(SettingsAnimations.exitTween()) + shrinkVertically(SettingsAnimations.exitTween()),
                    ) {
                        SettingsUpdateBanner(
                            latestVersion = state.latestVersion,
                            onClick = state.onUpdateClick,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }

            if (state.quickActions.isNotEmpty()) {
                item(key = "quickActions") {
                    AnimatedVisibility(
                        visible = quickActionsVisible,
                        enter = fadeIn(SettingsAnimations.entranceSpring()),
                    ) {
                        SettingsQuickActionsSection(
                            actions = state.quickActions,
                            columns = 2,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }

            if (state.integrations.isNotEmpty()) {
                item(key = "integrations") {
                    AnimatedVisibility(
                        visible = integrationsVisible,
                        enter = fadeIn(SettingsAnimations.entranceSpring()),
                    ) {
                        SettingsIntegrationsSection(
                            integrations = state.integrations,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(top = topPadding, bottom = 32.dp),
        ) {
            if (state.isSearchActive && !state.hasSearchResults) {
                item(key = "empty") {
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsSearchEmpty()
                }
            } else {
                if (state.internalGroup != null && state.internalGroup.items.isNotEmpty()) {
                    item(key = "internalSearchResults") {
                        SettingsGroupCard(
                            group = state.internalGroup,
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }

                items(
                    count = state.groups.size,
                    key = { state.groups[it].title },
                ) { index ->
                    AnimatedVisibility(
                        visible = categoriesVisible,
                        enter = fadeIn(
                            SettingsAnimations.staggerTween(index)
                        ) + slideInVertically(
                            initialOffsetY = { it / 5 },
                            animationSpec = SettingsAnimations.staggerTween(index),
                        ),
                    ) {
                        SettingsGroupCard(
                            group = state.groups[index],
                            modifier = Modifier.padding(bottom = spacing),
                        )
                    }
                }
            }
        }
    }
}
