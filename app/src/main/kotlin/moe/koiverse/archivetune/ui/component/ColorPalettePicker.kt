package moe.koiverse.archivetune.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.R

data class ColorPalettePreset(
    val id: String,
    val nameResId: Int,
    val primaryColor: Color,
    val accentColors: List<Color> = emptyList()
)

object ColorPalettePresets {

    val Default = ColorPalettePreset(
        id = "default",
        nameResId = R.string.palette_default,
        primaryColor = Color(0xFFED5564),
        accentColors = listOf(Color(0xFFFF6B7A), Color(0xFFD94452))
    )

    val OceanBlue = ColorPalettePreset(
        id = "ocean_blue",
        nameResId = R.string.palette_ocean_blue,
        primaryColor = Color(0xFF4A90D9),
        accentColors = listOf(Color(0xFF5BA0E9), Color(0xFF3A80C9))
    )

    val EmeraldGreen = ColorPalettePreset(
        id = "emerald_green",
        nameResId = R.string.palette_emerald_green,
        primaryColor = Color(0xFF2ECC71),
        accentColors = listOf(Color(0xFF3EDD81), Color(0xFF1EBC61))
    )

    val SunsetOrange = ColorPalettePreset(
        id = "sunset_orange",
        nameResId = R.string.palette_sunset_orange,
        primaryColor = Color(0xFFE67E22),
        accentColors = listOf(Color(0xFFF68E32), Color(0xFFD66E12))
    )

    val RoyalPurple = ColorPalettePreset(
        id = "royal_purple",
        nameResId = R.string.palette_royal_purple,
        primaryColor = Color(0xFF9B59B6),
        accentColors = listOf(Color(0xFFAB69C6), Color(0xFF8B49A6))
    )

    val CherryBlossom = ColorPalettePreset(
        id = "cherry_blossom",
        nameResId = R.string.palette_cherry_blossom,
        primaryColor = Color(0xFFFFB7C5),
        accentColors = listOf(Color(0xFFFFC7D5), Color(0xFFEFA7B5))
    )

    val MidnightNavy = ColorPalettePreset(
        id = "midnight_navy",
        nameResId = R.string.palette_midnight_navy,
        primaryColor = Color(0xFF2C3E50),
        accentColors = listOf(Color(0xFF3C4E60), Color(0xFF1C2E40))
    )

    val GoldenHour = ColorPalettePreset(
        id = "golden_hour",
        nameResId = R.string.palette_golden_hour,
        primaryColor = Color(0xFFF39C12),
        accentColors = listOf(Color(0xFFFFAC22), Color(0xFFE38C02))
    )

    val TealWave = ColorPalettePreset(
        id = "teal_wave",
        nameResId = R.string.palette_teal_wave,
        primaryColor = Color(0xFF1ABC9C),
        accentColors = listOf(Color(0xFF2ACCAC), Color(0xFF0AAC8C))
    )

    val RoseQuartz = ColorPalettePreset(
        id = "rose_quartz",
        nameResId = R.string.palette_rose_quartz,
        primaryColor = Color(0xFFF7CAC9),
        accentColors = listOf(Color(0xFFFFDAD9), Color(0xFFE7BAB9))
    )

    val LavenderDream = ColorPalettePreset(
        id = "lavender_dream",
        nameResId = R.string.palette_lavender_dream,
        primaryColor = Color(0xFFB39DDB),
        accentColors = listOf(Color(0xFFC3ADEB), Color(0xFFA38DCB))
    )

    val CrimsonRed = ColorPalettePreset(
        id = "crimson_red",
        nameResId = R.string.palette_crimson_red,
        primaryColor = Color(0xFFDC143C),
        accentColors = listOf(Color(0xFFEC244C), Color(0xFFCC042C))
    )

    val ForestGreen = ColorPalettePreset(
        id = "forest_green",
        nameResId = R.string.palette_forest_green,
        primaryColor = Color(0xFF228B22),
        accentColors = listOf(Color(0xFF329B32), Color(0xFF127B12))
    )

    val SpotifyGreen = ColorPalettePreset(
        id = "spotify_green",
        nameResId = R.string.palette_spotify_green,
        primaryColor = Color(0xFF1DB954),
        accentColors = listOf(Color(0xFF2DC964), Color(0xFF0DA944))
    )

    val YouTubeRed = ColorPalettePreset(
        id = "youtube_red",
        nameResId = R.string.palette_youtube_red,
        primaryColor = Color(0xFFFF0000),
        accentColors = listOf(Color(0xFFFF1010), Color(0xFFEF0000))
    )

    val ArcticBlue = ColorPalettePreset(
        id = "arctic_blue",
        nameResId = R.string.palette_arctic_blue,
        primaryColor = Color(0xFF00BFFF),
        accentColors = listOf(Color(0xFF10CFFF), Color(0xFF00AFEF))
    )

    val MagentaPop = ColorPalettePreset(
        id = "magenta_pop",
        nameResId = R.string.palette_magenta_pop,
        primaryColor = Color(0xFFFF00FF),
        accentColors = listOf(Color(0xFFFF10FF), Color(0xFFEF00EF))
    )

    val WarmAmber = ColorPalettePreset(
        id = "warm_amber",
        nameResId = R.string.palette_warm_amber,
        primaryColor = Color(0xFFFFBF00),
        accentColors = listOf(Color(0xFFFFCF10), Color(0xFFEFAF00))
    )

    val allPresets: List<ColorPalettePreset> = listOf(
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
        CrimsonRed,
        ForestGreen,
        SpotifyGreen,
        YouTubeRed,
        ArcticBlue,
        MagentaPop,
        WarmAmber
    )

    fun findPresetByColor(colorHex: String): ColorPalettePreset? {
        return allPresets.find { it.primaryColor.toHexString() == colorHex }
    }

    fun findPresetById(id: String): ColorPalettePreset? {
        return allPresets.find { it.id == id }
    }
}

fun Color.toHexString(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}

fun String.toColorOrNull(): Color? {
    return try {
        val colorString = this.removePrefix("#")
        Color(android.graphics.Color.parseColor("#$colorString"))
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ColorPalettePicker(
    modifier: Modifier = Modifier,
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    visible: Boolean = true
) {
    val selectedColor = remember(selectedColorHex) {
        selectedColorHex.toColorOrNull() ?: ColorPalettePresets.Default.primaryColor
    }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.palette),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.color_palette),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            CurrentColorPreview(
                selectedColor = selectedColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ColorPalettePresets.allPresets) { preset ->
                    ColorPaletteCard(
                        preset = preset,
                        isSelected = preset.primaryColor.toHexString() == selectedColorHex,
                        onClick = { onColorSelected(preset.primaryColor.toHexString()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CurrentColorPreview(
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = selectedColor,
        animationSpec = tween(durationMillis = 300),
        label = "colorAnimation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            animatedColor,
                            animatedColor.copy(alpha = 0.8f),
                            animatedColor.copy(alpha = 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.selected_theme_color),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (animatedColor.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) 
                               else Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = animatedColor.toHexString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (animatedColor.luminance() > 0.5f) Color.Black 
                               else Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(animatedColor)
                        .border(
                            width = 3.dp,
                            color = if (animatedColor.luminance() > 0.5f) Color.Black.copy(alpha = 0.2f) 
                                   else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun ColorPaletteCard(
    preset: ColorPalettePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scaleAnimation"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "borderColorAnimation"
    )

    Card(
        modifier = Modifier
            .width(100.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        brush = if (preset.accentColors.isNotEmpty()) {
                            Brush.linearGradient(
                                colors = listOf(preset.primaryColor) + preset.accentColors.take(1)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(preset.primaryColor, preset.primaryColor)
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = if (preset.primaryColor.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(preset.nameResId),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun QuickColorSelector(
    modifier: Modifier = Modifier,
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val quickColors = remember {
        listOf(
            ColorPalettePresets.Default,
            ColorPalettePresets.OceanBlue,
            ColorPalettePresets.EmeraldGreen,
            ColorPalettePresets.RoyalPurple,
            ColorPalettePresets.GoldenHour,
            ColorPalettePresets.SpotifyGreen
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        quickColors.forEach { preset ->
            val isSelected = preset.primaryColor.toHexString() == selectedColorHex
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "quickScaleAnimation"
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(scale)
                    .shadow(if (isSelected) 6.dp else 2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(preset.primaryColor)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary 
                               else Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(preset.primaryColor.toHexString()) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = if (preset.primaryColor.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}