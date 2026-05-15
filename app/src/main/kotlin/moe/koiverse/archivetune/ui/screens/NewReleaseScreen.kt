/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package moe.koiverse.archivetune.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.LocalPlayerConnection
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.GridThumbnailHeight
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.LocalMenuState
import moe.koiverse.archivetune.ui.component.YouTubeGridItem
import moe.koiverse.archivetune.ui.component.shimmer.GridItemPlaceHolder
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.menu.YouTubeAlbumMenu
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.viewmodels.NewReleaseUiState
import moe.koiverse.archivetune.viewmodels.NewReleaseViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NewReleaseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: NewReleaseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val newReleaseAlbums by viewModel.newReleaseAlbums.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.new_release_albums)) },
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
            )
        },
        contentWindowInsets = LocalPlayerAwareWindowInsets.current,
    ) { paddingValues ->
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(150))
            },
            modifier = Modifier.fillMaxSize(),
            label = "NewReleaseContent",
        ) { state ->
            when (state) {
                NewReleaseUiState.Loading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
                        contentPadding = paddingValues,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(12) {
                            ShimmerHost {
                                GridItemPlaceHolder(fillMaxWidth = true)
                            }
                        }
                    }
                }

                is NewReleaseUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
                        contentPadding = paddingValues,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = newReleaseAlbums.distinctBy { it.id },
                            key = { it.id },
                            contentType = { "album_grid_item" },
                        ) { album ->
                            YouTubeGridItem(
                                item = album,
                                isActive = mediaMetadata?.album?.id == album.id,
                                isPlaying = isPlaying,
                                fillMaxWidth = true,
                                coroutineScope = coroutineScope,
                                modifier = Modifier
                                    .animateItem()
                                    .combinedClickable(
                                        onClick = { navController.navigate("album/${album.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeAlbumMenu(
                                                    albumItem = album,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ),
                            )
                        }
                    }
                }

                is NewReleaseUiState.Error -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 24.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.error),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.error_unknown),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            state.throwable?.message?.let { errorMessage ->
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = viewModel::retry,
                                shapes = ButtonDefaults.shapes(),
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }

                NewReleaseUiState.Empty -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 24.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.no_results_found),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = viewModel::retry,
                                shapes = ButtonDefaults.shapes(),
                            ) {
                                Text(stringResource(R.string.refresh))
                            }
                        }
                    }
                }
            }
        }
    }
}
