package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.CustomThemeColorKey
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberPreference

/**
 * Data class representing a complete theme palette with Material-You style colors
 */
data class ThemePalette(
    val id: String,
    val nameResId: Int,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
    val onPrimary: Color = if (primary.luminance() > 0.5f) Color.Black else Color.White
)

/**
 * Predefined Material-You style theme palettes
 */
object ThemePalettes {
    
    val Default = ThemePalette(
        id = "default",
        nameResId = R.string.palette_default,
        primary = Color(0xFFED5564),
        secondary = Color(0xFFFF8A80),
        tertiary = Color(0xFFFFCDD2),
        neutral = Color(0xFF5D4037)
    )
    
    val OceanBlue = ThemePalette(
        id = "ocean_blue",
        nameResId = R.string.palette_ocean_blue,
        primary = Color(0xFF4A90D9),
        secondary = Color(0xFF82B1FF),
        tertiary = Color(0xFFBBDEFB),
        neutral = Color(0xFF37474F)
    )
    
    val EmeraldGreen = ThemePalette(
        id = "emerald_green",
        nameResId = R.string.palette_emerald_green,
        primary = Color(0xFF2ECC71),
        secondary = Color(0xFF69F0AE),
        tertiary = Color(0xFFB9F6CA),
        neutral = Color(0xFF2E7D32)
    )
    
    val SunsetOrange = ThemePalette(
        id = "sunset_orange",
        nameResId = R.string.palette_sunset_orange,
        primary = Color(0xFFE67E22),
        secondary = Color(0xFFFFAB40),
        tertiary = Color(0xFFFFE0B2),
        neutral = Color(0xFF795548)
    )
    
    val RoyalPurple = ThemePalette(
        id = "royal_purple",
        nameResId = R.string.palette_royal_purple,
        primary = Color(0xFF9B59B6),
        secondary = Color(0xFFCE93D8),
        tertiary = Color(0xFFF3E5F5),
        neutral = Color(0xFF4A148C)
    )
    
    val CherryBlossom = ThemePalette(
        id = "cherry_blossom",
        nameResId = R.string.palette_cherry_blossom,
        primary = Color(0xFFFFB7C5),
        secondary = Color(0xFFF8BBD9),
        tertiary = Color(0xFFFCE4EC),
        neutral = Color(0xFF880E4F)
    )
    
    val MidnightNavy = ThemePalette(
        id = "midnight_navy",
        nameResId = R.string.palette_midnight_navy,
        primary = Color(0xFF2C3E50),
        secondary = Color(0xFF546E7A),
        tertiary = Color(0xFF90A4AE),
        neutral = Color(0xFF1A237E)
    )
    
    val GoldenHour = ThemePalette(
        id = "golden_hour",
        nameResId = R.string.palette_golden_hour,
        primary = Color(0xFFF39C12),
        secondary = Color(0xFFFFD54F),
        tertiary = Color(0xFFFFF9C4),
        neutral = Color(0xFFFF6F00)
    )
    
    val TealWave = ThemePalette(
        id = "teal_wave",
        nameResId = R.string.palette_teal_wave,
        primary = Color(0xFF1ABC9C),
        secondary = Color(0xFF64FFDA),
        tertiary = Color(0xFFB2DFDB),
        neutral = Color(0xFF00695C)
    )
    
    val RoseQuartz = ThemePalette(
        id = "rose_quartz",
        nameResId = R.string.palette_rose_quartz,
        primary = Color(0xFFF7CAC9),
        secondary = Color(0xFFFFCCBC),
        tertiary = Color(0xFFFBE9E7),
        neutral = Color(0xFFBF360C)
    )
    
    val LavenderDream = ThemePalette(
        id = "lavender_dream",
        nameResId = R.string.palette_lavender_dream,
        primary = Color(0xFFB39DDB),
        secondary = Color(0xFFD1C4E9),
        tertiary = Color(0xFFEDE7F6),
        neutral = Color(0xFF512DA8)
    )
    
    val SpotifyGreen = ThemePalette(
        id = "spotify_green",
        nameResId = R.string.palette_spotify_green,
        primary = Color(0xFF1DB954),
        secondary = Color(0xFF1ED760),
        tertiary = Color(0xFFB3F5C3),
        neutral = Color(0xFF191414)
    )
    
    val allPalettes: List<ThemePalette> = listOf(
        Default,
        OceanBlue,
        EmeraldGreen,
        SunsetOrange,
        RoyalPurple,
        CherryBlossom,
        MidnightNavy,
        GoldenHour,
        TealWave,
        RoseQuartz,
        LavenderDream,
        SpotifyGreen
    )
    
    fun findByPrimaryColor(colorHex: String): ThemePalette? {
        return allPalettes.find { it.primary.toHexString() == colorHex }
    }
    
    fun findById(id: String): ThemePalette? {
        return allPalettes.find { it.id == id }
    }
}

/**
 * Extension function to convert Color to hex string
 */
private fun Color.toHexString(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}

/**
 * Main Palette Picker Screen with Material-You style UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalettePickerScreen(
    navController: NavController
) {
    val (customThemeColor, onCustomThemeColorChange) = rememberPreference(
        CustomThemeColorKey,
        defaultValue = ThemePalettes.Default.primary.toHexString()
    )
    
    val selectedPalette = remember(customThemeColor) {
        ThemePalettes.findByPrimaryColor(customThemeColor) ?: ThemePalettes.Default
    }
    
    val isDarkTheme = isSystemInDarkTheme()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.color_palette)) },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Large Preview Card
            ThemePreviewCard(
                palette = selectedPalette,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Section Title
            Text(
                text = stringResource(R.string.select_palette),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Color Palette Selector
            ColorPaletteSelector(
                palettes = ThemePalettes.allPalettes,
                selectedPalette = selectedPalette,
                onPaletteSelected = { palette ->
                    onCustomThemeColorChange(palette.primary.toHexString())
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Color Details Card
            SelectedPaletteDetails(
                palette = selectedPalette,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Large preview card showing an illustration with the selected palette
 */
@Composable
private fun ThemePreviewCard(
    palette: ThemePalette,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "primaryColor"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "secondaryColor"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = palette.tertiary,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "tertiaryColor"
    )
    val animatedNeutral by animateColorAsState(
        targetValue = palette.neutral,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "neutralColor"
    )
    
    val backgroundColor = if (isDarkTheme) {
        Color(0xFF1C1C1E)
    } else {
        animatedTertiary.copy(alpha = 0.3f)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background gradient
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.7f, size.height * 0.3f),
                    radius = size.width * 0.8f
                )
                drawRect(brush = gradientBrush)
            }
            
            // Illustration/Preview content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section - Mini app preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Mock player card
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = animatedPrimary.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Album art placeholder
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(animatedPrimary, animatedSecondary)
                                        )
                                    )
                            )
                            
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(animatedNeutral.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(animatedPrimary)
                                )
                            }
                        }
                    }
                    
                    // Floating action button preview
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(animatedPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = palette.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                // Middle section - Color orbs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Decorative color circles
                    listOf(
                        animatedPrimary to 48.dp,
                        animatedSecondary to 36.dp,
                        animatedTertiary to 28.dp
                    ).forEachIndexed { index, (color, size) ->
                        Box(
                            modifier = Modifier
                                .offset(x = (-12 * index).dp)
                                .size(size)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }
                
                // Bottom section - Chips/buttons preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(animatedPrimary, animatedSecondary, animatedNeutral).forEach { color ->
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color.copy(alpha = 0.2f))
                                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
            
            // Palette name badge
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = animatedPrimary,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = stringResource(palette.nameResId),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * Horizontal color palette selector with Material-You style cards
 */
@Composable
private fun ColorPaletteSelector(
    palettes: List<ThemePalette>,
    selectedPalette: ThemePalette,
    onPaletteSelected: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = palettes.indexOf(selectedPalette)
    
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(
                index = selectedIndex,
                scrollOffset = -100
            )
        }
    }
    
    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(palettes) { palette ->
            PaletteCard(
                palette = palette,
                isSelected = palette.id == selectedPalette.id,
                onClick = { onPaletteSelected(palette) }
            )
        }
    }
}

/**
 * Individual palette card with 4-segment color preview (Material-You style)
 */
@Composable
private fun PaletteCard(
    palette: ThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scaleAnimation"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "borderAnimation"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        animationSpec = tween(durationMillis = 200),
        label = "elevationAnimation"
    )
    
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) palette.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "borderColorAnimation"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .shadow(elevation, RoundedCornerShape(20.dp))
                .border(borderWidth, animatedBorderColor, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            // 4-segment color grid (Material-You style)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val halfWidth = size.width / 2
                    val halfHeight = size.height / 2
                    val cornerRadius = 12.dp.toPx()
                    
                    // Top-left - Primary
                    drawRoundRect(
                        color = palette.primary,
                        topLeft = Offset.Zero,
                        size = Size(halfWidth - 2, halfHeight - 2),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                    
                    // Top-right - Secondary
                    drawRoundRect(
                        color = palette.secondary,
                        topLeft = Offset(halfWidth + 2, 0f),
                        size = Size(halfWidth - 2, halfHeight - 2),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                    
                    // Bottom-left - Tertiary
                    drawRoundRect(
                        color = palette.tertiary,
                        topLeft = Offset(0f, halfHeight + 2),
                        size = Size(halfWidth - 2, halfHeight - 2),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                    
                    // Bottom-right - Neutral
                    drawRoundRect(
                        color = palette.neutral,
                        topLeft = Offset(halfWidth + 2, halfHeight + 2),
                        size = Size(halfWidth - 2, halfHeight - 2),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                }
                
                // Selection checkmark
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(palette.nameResId),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) palette.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Shows details about the selected palette with color swatches
 */
@Composable
private fun SelectedPaletteDetails(
    palette: ThemePalette,
    modifier: Modifier = Modifier
) {
    val animatedPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(durationMillis = 400),
        label = "detailPrimary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(durationMillis = 400),
        label = "detailSecondary"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = palette.tertiary,
        animationSpec = tween(durationMillis = 400),
        label = "detailTertiary"
    )
    val animatedNeutral by animateColorAsState(
        targetValue = palette.neutral,
        animationSpec = tween(durationMillis = 400),
        label = "detailNeutral"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.selected_theme_color),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ColorSwatch(
                    color = animatedPrimary,
                    label = "Primary",
                    hexCode = palette.primary.toHexString()
                )
                ColorSwatch(
                    color = animatedSecondary,
                    label = "Secondary",
                    hexCode = palette.secondary.toHexString()
                )
                ColorSwatch(
                    color = animatedTertiary,
                    label = "Tertiary",
                    hexCode = palette.tertiary.toHexString()
                )
                ColorSwatch(
                    color = animatedNeutral,
                    label = "Neutral",
                    hexCode = palette.neutral.toHexString()
                )
            }
        }
    }
}

/**
 * Individual color swatch with label and hex code
 */
@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
    hexCode: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = hexCode,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Reusable Color Palette Picker component
 */
@Composable
fun ColorPalettePicker(
    palettes: List<ThemePalette>,
    selectedPalette: ThemePalette,
    onPaletteSelected: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier,
    showPreview: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showPreview) {
            ThemePreviewCard(
                palette = selectedPalette,
                isDarkTheme = isSystemInDarkTheme(),
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        ColorPaletteSelector(
            palettes = palettes,
            selectedPalette = selectedPalette,
            onPaletteSelected = onPaletteSelected
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PalettePickerScreenPreview() {
    MaterialTheme {
        PalettePickerScreen(
            navController = rememberNavController()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PaletteCardPreview() {
    MaterialTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            PaletteCard(
                palette = ThemePalettes.Default,
                isSelected = true,
                onClick = {}
            )
            PaletteCard(
                palette = ThemePalettes.OceanBlue,
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemePreviewCardPreview() {
    MaterialTheme {
        ThemePreviewCard(
            palette = ThemePalettes.EmeraldGreen,
            isDarkTheme = false,
            modifier = Modifier.padding(24.dp)
        )
    }
}
