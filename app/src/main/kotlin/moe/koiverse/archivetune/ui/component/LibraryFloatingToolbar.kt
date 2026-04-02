@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.LibraryViewType
import moe.koiverse.archivetune.constants.PlaylistSortType
import moe.koiverse.archivetune.constants.PlaylistSongSortType

@Composable
inline fun <reified T : Enum<T>> BoxScope.LibraryFloatingToolbar(
    sortType: T,
    sortDescending: Boolean,
    crossinline onSortTypeChange: (T) -> Unit,
    crossinline onSortDescendingChange: (Boolean) -> Unit,
    crossinline sortTypeText: (T) -> Int,
    viewType: LibraryViewType? = null,
    crossinline onViewTypeToggle: () -> Unit = {},
    scrollBehavior: FloatingToolbarScrollBehavior,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    itemCountText: String? = null,
    canReorder: Boolean = false,
    reorderEnabled: Boolean = false,
    crossinline onReorderToggle: () -> Unit = {},
    @DrawableRes fabIcon: Int? = null,
    crossinline onFabClick: () -> Unit = {},
) {
    val containerColor = libraryToolbarContainerColor(pureBlack)
    val colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
        toolbarContainerColor = containerColor,
    )

    val toolbarContent: @Composable () -> Unit = {
        SortDropdown(
            sortType = sortType,
            onSortTypeChange = onSortTypeChange,
            sortTypeText = sortTypeText,
            pureBlack = pureBlack,
        )

        val allowDescending = when (sortType) {
            is PlaylistSongSortType -> sortType != PlaylistSongSortType.CUSTOM
            is PlaylistSortType -> sortType != PlaylistSortType.CUSTOM
            else -> true
        }

        if (allowDescending) {
            IconButton(onClick = { onSortDescendingChange(!sortDescending) }) {
                Icon(
                    painter = painterResource(
                        if (sortDescending) R.drawable.arrow_downward else R.drawable.arrow_upward,
                    ),
                    contentDescription = null,
                    tint = libraryToolbarContentColor(pureBlack),
                )
            }
        }

        if (itemCountText != null) {
            Text(
                text = itemCountText,
                style = MaterialTheme.typography.labelMedium,
                color = libraryToolbarContentColor(pureBlack).copy(alpha = 0.7f),
            )
        }

        if (canReorder) {
            IconButton(onClick = { onReorderToggle() }) {
                Icon(
                    painter = painterResource(
                        if (reorderEnabled) R.drawable.lock_open else R.drawable.lock,
                    ),
                    contentDescription = null,
                    tint = libraryToolbarContentColor(pureBlack),
                )
            }
        }

        if (viewType != null) {
            IconButton(onClick = { onViewTypeToggle() }) {
                Icon(
                    painter = painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        },
                    ),
                    contentDescription = null,
                    tint = libraryToolbarContentColor(pureBlack),
                )
            }
        }
    }

    if (fabIcon != null) {
        HorizontalFloatingToolbar(
            expanded = true,
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = { onFabClick() },
                    containerColor = libraryToolbarFabContainerColor(pureBlack),
                    contentColor = libraryToolbarFabContentColor(pureBlack),
                ) {
                    Icon(
                        painter = painterResource(fabIcon),
                        contentDescription = null,
                    )
                }
            },
            modifier = modifier
                .align(Alignment.BottomCenter)
                .offset(y = -ScreenOffset)
                .zIndex(1f)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
            colors = colors,
            scrollBehavior = scrollBehavior,
            content = { toolbarContent() },
        )
    } else {
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = modifier
                .align(Alignment.BottomCenter)
                .offset(y = -ScreenOffset)
                .zIndex(1f)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
            colors = colors,
            scrollBehavior = scrollBehavior,
            content = { toolbarContent() },
        )
    }
}

@Composable
inline fun <reified T : Enum<T>> SortDropdown(
    sortType: T,
    crossinline onSortTypeChange: (T) -> Unit,
    crossinline sortTypeText: (T) -> Int,
    pureBlack: Boolean = false,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TextButton(
        onClick = { menuExpanded = true },
        colors = ButtonDefaults.textButtonColors(
            contentColor = libraryToolbarContentColor(pureBlack),
        ),
    ) {
        Text(
            text = stringResource(sortTypeText(sortType)),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
        Icon(
            painter = painterResource(R.drawable.expand_more),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
    }

    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
    ) {
        enumValues<T>().forEach { type ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(sortTypeText(type)),
                        fontSize = 14.sp,
                        fontWeight = if (sortType == type) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(
                            if (sortType == type) R.drawable.radio_button_checked
                            else R.drawable.radio_button_unchecked,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                onClick = {
                    onSortTypeChange(type)
                    menuExpanded = false
                },
            )
        }
    }
}

@Composable
fun libraryToolbarContainerColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
}

@Composable
fun libraryToolbarContentColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun libraryToolbarFabContainerColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiaryContainer
}

@Composable
fun libraryToolbarFabContentColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White else MaterialTheme.colorScheme.onTertiaryContainer
}
