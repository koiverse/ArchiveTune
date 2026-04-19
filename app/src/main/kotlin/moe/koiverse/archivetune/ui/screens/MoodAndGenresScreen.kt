/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.maskBorder
import moe.koiverse.archivetune.ui.component.maskClip
import moe.koiverse.archivetune.ui.component.shimmer.ShimmerHost
import moe.koiverse.archivetune.ui.component.shimmer.TextPlaceholder
import moe.koiverse.archivetune.viewmodels.MoodAndGenresViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val moodAndGenres by viewModel.moodAndGenres.collectAsState()
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val topPadding = with(density) { windowInsets.getTop(this).toDp() }
    val bottomPadding = with(density) { windowInsets.getBottom(this).toDp() }
    val moodItems = moodAndGenres.orEmpty()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            gridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 198.dp),
        state = gridState,
        contentPadding = PaddingValues(
            start = 12.dp,
            top = topPadding,
            end = 12.dp,
            bottom = bottomPadding + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            MoodAndGenresHero(
                itemCount = moodItems.size,
                modifier = Modifier.animateItem(),
            )
        }

        if (moodAndGenres == null) {
            items(12) {
                ShimmerHost {
                    TextPlaceholder(
                        height = MoodAndGenresTabButtonHeight,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        } else {
            items(
                items = moodItems,
                key = { item -> "${item.title}:${item.endpoint.browseId}:${item.endpoint.params}" },
            ) { item ->
                MoodAndGenresTabButton(
                    title = item.title,
                    stripeColor = item.stripeColor,
                    onClick = {
                        navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }
    }
}

@Composable
private fun MoodAndGenresHero(
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.mood_and_genres)
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
        ),
        start = Offset.Zero,
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 520.dp
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .maskClip(MaterialTheme.shapes.extraLarge)
                .maskBorder(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    MaterialTheme.shapes.extraLarge,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(horizontal = 22.dp, vertical = 24.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(if (isWide) 0.72f else 1f),
                ) {
                    if (itemCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = CircleShape,
                        ) {
                            Text(
                                text = stringResource(R.string.mood_and_genres_count, itemCount),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )

                    Text(
                        text = stringResource(R.string.mood_and_genres_supporting),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.style),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(18.dp).size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun MoodAndGenresButton(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoodAndGenresCompactButton(
        title = title,
        stripeColor = stripeColor,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun MoodAndGenresCompactButton(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = Color(stripeColor)
    val darkVariant = Color(
        red = (base.red * 0.65f).coerceIn(0f, 1f),
        green = (base.green * 0.65f).coerceIn(0f, 1f),
        blue = (base.blue * 0.65f).coerceIn(0f, 1f),
    )
    val gradient = Brush.linearGradient(
        colors = listOf(base, darkVariant),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )
    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = modifier
            .height(MoodAndGenresButtonHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.35f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f,
                ),
            ),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MoodAndGenresTabButton(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = Color(stripeColor)
    val softened = Color(
        red = (base.red * 0.84f + MaterialTheme.colorScheme.surface.red * 0.16f).coerceIn(0f, 1f),
        green = (base.green * 0.84f + MaterialTheme.colorScheme.surface.green * 0.16f).coerceIn(0f, 1f),
        blue = (base.blue * 0.84f + MaterialTheme.colorScheme.surface.blue * 0.16f).coerceIn(0f, 1f),
    )
    val gradient = Brush.verticalGradient(
        colors = listOf(
            softened,
            base,
            Color(
                red = (base.red * 0.72f).coerceIn(0f, 1f),
                green = (base.green * 0.72f).coerceIn(0f, 1f),
                blue = (base.blue * 0.72f).coerceIn(0f, 1f),
            ),
        ),
    )

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = base),
        modifier = modifier
            .height(MoodAndGenresTabButtonHeight)
            .maskClip(MaterialTheme.shapes.extraLarge)
            .maskBorder(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                MaterialTheme.shapes.extraLarge,
            )
            .combinedClickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MoodAndGenresTabButtonHeight)
                .background(gradient)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Spacer(Modifier.size(30.dp))
            }

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

val MoodAndGenresButtonHeight = 88.dp
private val MoodAndGenresTabButtonHeight = 116.dp
