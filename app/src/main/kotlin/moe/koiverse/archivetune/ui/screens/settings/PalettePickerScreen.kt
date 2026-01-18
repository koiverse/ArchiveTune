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
import androidx.compose.runtime.derivedStateOf
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

data class ThemePalette(
    val id: String,
    val nameResId: Int,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
    val onPrimary: Color = if (primary.luminance() > 0.5f) Color.Black else Color.White
)

object ThemePalettes {
    
    // ===== Classic & Default =====
    val Default = ThemePalette(
        id = "default",
        nameResId = R.string.palette_default,
        primary = Color(0xFFED5564),
        secondary = Color(0xFFFF8A80),
        tertiary = Color(0xFFFFCDD2),
        neutral = Color(0xFF5D4037)
    )
    
    // ===== Blues =====
    val OceanBlue = ThemePalette(
        id = "ocean_blue",
        nameResId = R.string.palette_ocean_blue,
        primary = Color(0xFF4A90D9),
        secondary = Color(0xFF82B1FF),
        tertiary = Color(0xFFBBDEFB),
        neutral = Color(0xFF37474F)
    )
    
    val ArcticBlue = ThemePalette(
        id = "arctic_blue",
        nameResId = R.string.palette_arctic_blue,
        primary = Color(0xFF00BFFF),
        secondary = Color(0xFF4DD0E1),
        tertiary = Color(0xFFB2EBF2),
        neutral = Color(0xFF006064)
    )
    
    val MidnightNavy = ThemePalette(
        id = "midnight_navy",
        nameResId = R.string.palette_midnight_navy,
        primary = Color(0xFF2C3E50),
        secondary = Color(0xFF546E7A),
        tertiary = Color(0xFF90A4AE),
        neutral = Color(0xFF1A237E)
    )
    
    val SkyBlue = ThemePalette(
        id = "sky_blue",
        nameResId = R.string.palette_sky_blue,
        primary = Color(0xFF87CEEB),
        secondary = Color(0xFFB3E5FC),
        tertiary = Color(0xFFE1F5FE),
        neutral = Color(0xFF0277BD)
    )
    
    val CobaltBlue = ThemePalette(
        id = "cobalt_blue",
        nameResId = R.string.palette_cobalt_blue,
        primary = Color(0xFF0047AB),
        secondary = Color(0xFF5472D3),
        tertiary = Color(0xFFC5CAE9),
        neutral = Color(0xFF1A237E)
    )
    
    val ElectricBlue = ThemePalette(
        id = "electric_blue",
        nameResId = R.string.palette_electric_blue,
        primary = Color(0xFF7DF9FF),
        secondary = Color(0xFFB2FEFA),
        tertiary = Color(0xFFE0FFFF),
        neutral = Color(0xFF00838F)
    )
    
    // ===== Greens =====
    val EmeraldGreen = ThemePalette(
        id = "emerald_green",
        nameResId = R.string.palette_emerald_green,
        primary = Color(0xFF2ECC71),
        secondary = Color(0xFF69F0AE),
        tertiary = Color(0xFFB9F6CA),
        neutral = Color(0xFF2E7D32)
    )
    
    val TealWave = ThemePalette(
        id = "teal_wave",
        nameResId = R.string.palette_teal_wave,
        primary = Color(0xFF1ABC9C),
        secondary = Color(0xFF64FFDA),
        tertiary = Color(0xFFB2DFDB),
        neutral = Color(0xFF00695C)
    )
    
    val ForestGreen = ThemePalette(
        id = "forest_green",
        nameResId = R.string.palette_forest_green,
        primary = Color(0xFF228B22),
        secondary = Color(0xFF66BB6A),
        tertiary = Color(0xFFC8E6C9),
        neutral = Color(0xFF1B5E20)
    )
    
    val SpotifyGreen = ThemePalette(
        id = "spotify_green",
        nameResId = R.string.palette_spotify_green,
        primary = Color(0xFF1DB954),
        secondary = Color(0xFF1ED760),
        tertiary = Color(0xFFB3F5C3),
        neutral = Color(0xFF191414)
    )
    
    val MintFresh = ThemePalette(
        id = "mint_fresh",
        nameResId = R.string.palette_mint_fresh,
        primary = Color(0xFF98FF98),
        secondary = Color(0xFFB2FFAB),
        tertiary = Color(0xFFE8F5E9),
        neutral = Color(0xFF2E7D32)
    )
    
    val OliveGarden = ThemePalette(
        id = "olive_garden",
        nameResId = R.string.palette_olive_garden,
        primary = Color(0xFF808000),
        secondary = Color(0xFFAFB42B),
        tertiary = Color(0xFFF0F4C3),
        neutral = Color(0xFF33691E)
    )
    
    val SageGreen = ThemePalette(
        id = "sage_green",
        nameResId = R.string.palette_sage_green,
        primary = Color(0xFF9CAF88),
        secondary = Color(0xFFB5C99A),
        tertiary = Color(0xFFDCEDC8),
        neutral = Color(0xFF558B2F)
    )
    
    // ===== Oranges & Yellows =====
    val SunsetOrange = ThemePalette(
        id = "sunset_orange",
        nameResId = R.string.palette_sunset_orange,
        primary = Color(0xFFE67E22),
        secondary = Color(0xFFFFAB40),
        tertiary = Color(0xFFFFE0B2),
        neutral = Color(0xFF795548)
    )
    
    val GoldenHour = ThemePalette(
        id = "golden_hour",
        nameResId = R.string.palette_golden_hour,
        primary = Color(0xFFF39C12),
        secondary = Color(0xFFFFD54F),
        tertiary = Color(0xFFFFF9C4),
        neutral = Color(0xFFFF6F00)
    )
    
    val WarmAmber = ThemePalette(
        id = "warm_amber",
        nameResId = R.string.palette_warm_amber,
        primary = Color(0xFFFFBF00),
        secondary = Color(0xFFFFCA28),
        tertiary = Color(0xFFFFECB3),
        neutral = Color(0xFFFF8F00)
    )
    
    val TangerineBlast = ThemePalette(
        id = "tangerine_blast",
        nameResId = R.string.palette_tangerine_blast,
        primary = Color(0xFFFF9800),
        secondary = Color(0xFFFFB74D),
        tertiary = Color(0xFFFFE0B2),
        neutral = Color(0xFFE65100)
    )
    
    val Peach = ThemePalette(
        id = "peach",
        nameResId = R.string.palette_peach,
        primary = Color(0xFFFFDAB9),
        secondary = Color(0xFFFFE4C4),
        tertiary = Color(0xFFFFF3E0),
        neutral = Color(0xFFBF360C)
    )
    
    val Mango = ThemePalette(
        id = "mango",
        nameResId = R.string.palette_mango,
        primary = Color(0xFFFF8243),
        secondary = Color(0xFFFFAB91),
        tertiary = Color(0xFFFFCCBC),
        neutral = Color(0xFFD84315)
    )
    
    // ===== Purples & Violets =====
    val RoyalPurple = ThemePalette(
        id = "royal_purple",
        nameResId = R.string.palette_royal_purple,
        primary = Color(0xFF9B59B6),
        secondary = Color(0xFFCE93D8),
        tertiary = Color(0xFFF3E5F5),
        neutral = Color(0xFF4A148C)
    )
    
    val LavenderDream = ThemePalette(
        id = "lavender_dream",
        nameResId = R.string.palette_lavender_dream,
        primary = Color(0xFFB39DDB),
        secondary = Color(0xFFD1C4E9),
        tertiary = Color(0xFFEDE7F6),
        neutral = Color(0xFF512DA8)
    )
    
    val GrapePurple = ThemePalette(
        id = "grape_purple",
        nameResId = R.string.palette_grape_purple,
        primary = Color(0xFF6B5B95),
        secondary = Color(0xFF9575CD),
        tertiary = Color(0xFFD1C4E9),
        neutral = Color(0xFF311B92)
    )
    
    val Violet = ThemePalette(
        id = "violet",
        nameResId = R.string.palette_violet,
        primary = Color(0xFFEE82EE),
        secondary = Color(0xFFE1BEE7),
        tertiary = Color(0xFFF3E5F5),
        neutral = Color(0xFF7B1FA2)
    )
    
    val Amethyst = ThemePalette(
        id = "amethyst",
        nameResId = R.string.palette_amethyst,
        primary = Color(0xFF9966CC),
        secondary = Color(0xFFB39DDB),
        tertiary = Color(0xFFE1BEE7),
        neutral = Color(0xFF6A1B9A)
    )
    
    val UltraViolet = ThemePalette(
        id = "ultra_violet",
        nameResId = R.string.palette_ultra_violet,
        primary = Color(0xFF645394),
        secondary = Color(0xFF9575CD),
        tertiary = Color(0xFFD1C4E9),
        neutral = Color(0xFF4527A0)
    )
    
    // ===== Pinks & Roses =====
    val CherryBlossom = ThemePalette(
        id = "cherry_blossom",
        nameResId = R.string.palette_cherry_blossom,
        primary = Color(0xFFFFB7C5),
        secondary = Color(0xFFF8BBD9),
        tertiary = Color(0xFFFCE4EC),
        neutral = Color(0xFF880E4F)
    )
    
    val RoseQuartz = ThemePalette(
        id = "rose_quartz",
        nameResId = R.string.palette_rose_quartz,
        primary = Color(0xFFF7CAC9),
        secondary = Color(0xFFFFCCBC),
        tertiary = Color(0xFFFBE9E7),
        neutral = Color(0xFFBF360C)
    )
    
    val MagentaPop = ThemePalette(
        id = "magenta_pop",
        nameResId = R.string.palette_magenta_pop,
        primary = Color(0xFFFF00FF),
        secondary = Color(0xFFFF80AB),
        tertiary = Color(0xFFFCE4EC),
        neutral = Color(0xFFAD1457)
    )
    
    val HotPink = ThemePalette(
        id = "hot_pink",
        nameResId = R.string.palette_hot_pink,
        primary = Color(0xFFFF69B4),
        secondary = Color(0xFFF48FB1),
        tertiary = Color(0xFFF8BBD9),
        neutral = Color(0xFFC2185B)
    )
    
    val Blush = ThemePalette(
        id = "blush",
        nameResId = R.string.palette_blush,
        primary = Color(0xFFDE5D83),
        secondary = Color(0xFFF48FB1),
        tertiary = Color(0xFFFCE4EC),
        neutral = Color(0xFF880E4F)
    )
    
    val Coral = ThemePalette(
        id = "coral",
        nameResId = R.string.palette_coral,
        primary = Color(0xFFFF7F50),
        secondary = Color(0xFFFF8A65),
        tertiary = Color(0xFFFFCCBC),
        neutral = Color(0xFFBF360C)
    )
    
    val Bubblegum = ThemePalette(
        id = "bubblegum",
        nameResId = R.string.palette_bubblegum,
        primary = Color(0xFFFFC1CC),
        secondary = Color(0xFFFFD6E0),
        tertiary = Color(0xFFFFF0F5),
        neutral = Color(0xFFD81B60)
    )
    
    // ===== Reds =====
    val CrimsonRed = ThemePalette(
        id = "crimson_red",
        nameResId = R.string.palette_crimson_red,
        primary = Color(0xFFDC143C),
        secondary = Color(0xFFEF5350),
        tertiary = Color(0xFFFFCDD2),
        neutral = Color(0xFFB71C1C)
    )
    
    val YouTubeRed = ThemePalette(
        id = "youtube_red",
        nameResId = R.string.palette_youtube_red,
        primary = Color(0xFFFF0000),
        secondary = Color(0xFFFF5252),
        tertiary = Color(0xFFFFCDD2),
        neutral = Color(0xFF282828)
    )
    
    val WineRed = ThemePalette(
        id = "wine_red",
        nameResId = R.string.palette_wine_red,
        primary = Color(0xFF722F37),
        secondary = Color(0xFFAD4A5B),
        tertiary = Color(0xFFEFCFD4),
        neutral = Color(0xFF4A0E0E)
    )
    
    val RubyRed = ThemePalette(
        id = "ruby_red",
        nameResId = R.string.palette_ruby_red,
        primary = Color(0xFFE0115F),
        secondary = Color(0xFFEC407A),
        tertiary = Color(0xFFF8BBD9),
        neutral = Color(0xFF880E4F)
    )
    
    val Scarlet = ThemePalette(
        id = "scarlet",
        nameResId = R.string.palette_scarlet,
        primary = Color(0xFFFF2400),
        secondary = Color(0xFFFF5722),
        tertiary = Color(0xFFFFCCBC),
        neutral = Color(0xFFBF360C)
    )
    
    // ===== Neutrals & Monochrome =====
    val Charcoal = ThemePalette(
        id = "charcoal",
        nameResId = R.string.palette_charcoal,
        primary = Color(0xFF36454F),
        secondary = Color(0xFF607D8B),
        tertiary = Color(0xFFCFD8DC),
        neutral = Color(0xFF263238)
    )
    
    val Silver = ThemePalette(
        id = "silver",
        nameResId = R.string.palette_silver,
        primary = Color(0xFFC0C0C0),
        secondary = Color(0xFFE0E0E0),
        tertiary = Color(0xFFF5F5F5),
        neutral = Color(0xFF424242)
    )
    
    val Slate = ThemePalette(
        id = "slate",
        nameResId = R.string.palette_slate,
        primary = Color(0xFF708090),
        secondary = Color(0xFF90A4AE),
        tertiary = Color(0xFFCFD8DC),
        neutral = Color(0xFF455A64)
    )
    
    val Graphite = ThemePalette(
        id = "graphite",
        nameResId = R.string.palette_graphite,
        primary = Color(0xFF474747),
        secondary = Color(0xFF757575),
        tertiary = Color(0xFFBDBDBD),
        neutral = Color(0xFF212121)
    )
    
    // ===== Earth Tones =====
    val Terracotta = ThemePalette(
        id = "terracotta",
        nameResId = R.string.palette_terracotta,
        primary = Color(0xFFE2725B),
        secondary = Color(0xFFFFAB91),
        tertiary = Color(0xFFFFCCBC),
        neutral = Color(0xFFBF360C)
    )
    
    val Coffee = ThemePalette(
        id = "coffee",
        nameResId = R.string.palette_coffee,
        primary = Color(0xFF6F4E37),
        secondary = Color(0xFF8D6E63),
        tertiary = Color(0xFFD7CCC8),
        neutral = Color(0xFF3E2723)
    )
    
    val Mocha = ThemePalette(
        id = "mocha",
        nameResId = R.string.palette_mocha,
        primary = Color(0xFF967969),
        secondary = Color(0xFFA1887F),
        tertiary = Color(0xFFD7CCC8),
        neutral = Color(0xFF5D4037)
    )
    
    val Sand = ThemePalette(
        id = "sand",
        nameResId = R.string.palette_sand,
        primary = Color(0xFFC2B280),
        secondary = Color(0xFFD7CCC8),
        tertiary = Color(0xFFEFEBE9),
        neutral = Color(0xFF795548)
    )
    
    val Clay = ThemePalette(
        id = "clay",
        nameResId = R.string.palette_clay,
        primary = Color(0xFFB66A50),
        secondary = Color(0xFFBCAAA4),
        tertiary = Color(0xFFEFEBE9),
        neutral = Color(0xFF5D4037)
    )
    
    // ===== Pastels =====
    val PastelPink = ThemePalette(
        id = "pastel_pink",
        nameResId = R.string.palette_pastel_pink,
        primary = Color(0xFFFFD1DC),
        secondary = Color(0xFFFFE4E9),
        tertiary = Color(0xFFFFF5F7),
        neutral = Color(0xFFE91E63)
    )
    
    val PastelBlue = ThemePalette(
        id = "pastel_blue",
        nameResId = R.string.palette_pastel_blue,
        primary = Color(0xFFAEC6CF),
        secondary = Color(0xFFD4E5ED),
        tertiary = Color(0xFFECF4F8),
        neutral = Color(0xFF1976D2)
    )
    
    val PastelGreen = ThemePalette(
        id = "pastel_green",
        nameResId = R.string.palette_pastel_green,
        primary = Color(0xFF77DD77),
        secondary = Color(0xFFB5EAB5),
        tertiary = Color(0xFFE8F5E9),
        neutral = Color(0xFF388E3C)
    )
    
    val PastelYellow = ThemePalette(
        id = "pastel_yellow",
        nameResId = R.string.palette_pastel_yellow,
        primary = Color(0xFFFDFD96),
        secondary = Color(0xFFFFF9C4),
        tertiary = Color(0xFFFFFDE7),
        neutral = Color(0xFFF9A825)
    )
    
    val PastelPurple = ThemePalette(
        id = "pastel_purple",
        nameResId = R.string.palette_pastel_purple,
        primary = Color(0xFFB19CD9),
        secondary = Color(0xFFD1C4E9),
        tertiary = Color(0xFFEDE7F6),
        neutral = Color(0xFF7B1FA2)
    )
    
    // ===== Neon & Vibrant =====
    val NeonGreen = ThemePalette(
        id = "neon_green",
        nameResId = R.string.palette_neon_green,
        primary = Color(0xFF39FF14),
        secondary = Color(0xFF76FF03),
        tertiary = Color(0xFFCCFF90),
        neutral = Color(0xFF33691E)
    )
    
    val NeonPink = ThemePalette(
        id = "neon_pink",
        nameResId = R.string.palette_neon_pink,
        primary = Color(0xFFFF10F0),
        secondary = Color(0xFFFF80AB),
        tertiary = Color(0xFFFCE4EC),
        neutral = Color(0xFF880E4F)
    )
    
    val NeonBlue = ThemePalette(
        id = "neon_blue",
        nameResId = R.string.palette_neon_blue,
        primary = Color(0xFF00F5FF),
        secondary = Color(0xFF18FFFF),
        tertiary = Color(0xFFB2EBF2),
        neutral = Color(0xFF006064)
    )
    
    val NeonOrange = ThemePalette(
        id = "neon_orange",
        nameResId = R.string.palette_neon_orange,
        primary = Color(0xFFFF5F1F),
        secondary = Color(0xFFFF8A65),
        tertiary = Color(0xFFFFCCBC),
        neutral = Color(0xFFE65100)
    )
    
    val Cyberpunk = ThemePalette(
        id = "cyberpunk",
        nameResId = R.string.palette_cyberpunk,
        primary = Color(0xFFFF00FF),
        secondary = Color(0xFF00FFFF),
        tertiary = Color(0xFFFFFF00),
        neutral = Color(0xFF0D0D0D)
    )
    
    val Synthwave = ThemePalette(
        id = "synthwave",
        nameResId = R.string.palette_synthwave,
        primary = Color(0xFFFF6EC7),
        secondary = Color(0xFF9D4EDD),
        tertiary = Color(0xFF3C096C),
        neutral = Color(0xFF10002B)
    )
    
    // ===== Special & Themed =====
    val Ocean = ThemePalette(
        id = "ocean",
        nameResId = R.string.palette_ocean,
        primary = Color(0xFF006994),
        secondary = Color(0xFF40E0D0),
        tertiary = Color(0xFFB2DFDB),
        neutral = Color(0xFF004D40)
    )
    
    val Forest = ThemePalette(
        id = "forest",
        nameResId = R.string.palette_forest,
        primary = Color(0xFF0B3D0B),
        secondary = Color(0xFF4CAF50),
        tertiary = Color(0xFFC8E6C9),
        neutral = Color(0xFF1B5E20)
    )
    
    val Autumn = ThemePalette(
        id = "autumn",
        nameResId = R.string.palette_autumn,
        primary = Color(0xFFD2691E),
        secondary = Color(0xFFFF8C00),
        tertiary = Color(0xFFFFE4B5),
        neutral = Color(0xFF8B4513)
    )
    
    val Winter = ThemePalette(
        id = "winter",
        nameResId = R.string.palette_winter,
        primary = Color(0xFFADD8E6),
        secondary = Color(0xFFB0E0E6),
        tertiary = Color(0xFFF0FFFF),
        neutral = Color(0xFF4682B4)
    )
    
    val Spring = ThemePalette(
        id = "spring",
        nameResId = R.string.palette_spring,
        primary = Color(0xFF98FB98),
        secondary = Color(0xFFFFB6C1),
        tertiary = Color(0xFFFFF0F5),
        neutral = Color(0xFF3CB371)
    )
    
    val Summer = ThemePalette(
        id = "summer",
        nameResId = R.string.palette_summer,
        primary = Color(0xFFFFD700),
        secondary = Color(0xFFFF6347),
        tertiary = Color(0xFFFFFACD),
        neutral = Color(0xFFFF4500)
    )
    
    val Twilight = ThemePalette(
        id = "twilight",
        nameResId = R.string.palette_twilight,
        primary = Color(0xFF4B0082),
        secondary = Color(0xFF8A2BE2),
        tertiary = Color(0xFFDDA0DD),
        neutral = Color(0xFF2E0854)
    )
    
    val Aurora = ThemePalette(
        id = "aurora",
        nameResId = R.string.palette_aurora,
        primary = Color(0xFF00FF7F),
        secondary = Color(0xFF00CED1),
        tertiary = Color(0xFFDA70D6),
        neutral = Color(0xFF191970)
    )
    
    val Candy = ThemePalette(
        id = "candy",
        nameResId = R.string.palette_candy,
        primary = Color(0xFFFF69B4),
        secondary = Color(0xFF00BFFF),
        tertiary = Color(0xFFFFE4E1),
        neutral = Color(0xFFFF1493)
    )
    
    val Rainbow = ThemePalette(
        id = "rainbow",
        nameResId = R.string.palette_rainbow,
        primary = Color(0xFFFF0000),
        secondary = Color(0xFFFFFF00),
        tertiary = Color(0xFF00FF00),
        neutral = Color(0xFF0000FF)
    )
    
    val allPalettes: List<ThemePalette> = listOf(
        // Classic
        Default,
        // Blues
        OceanBlue,
        ArcticBlue,
        MidnightNavy,
        SkyBlue,
        CobaltBlue,
        ElectricBlue,
        // Greens
        EmeraldGreen,
        TealWave,
        ForestGreen,
        SpotifyGreen,
        MintFresh,
        OliveGarden,
        SageGreen,
        // Oranges & Yellows
        SunsetOrange,
        GoldenHour,
        WarmAmber,
        TangerineBlast,
        Peach,
        Mango,
        // Purples
        RoyalPurple,
        LavenderDream,
        GrapePurple,
        Violet,
        Amethyst,
        UltraViolet,
        // Pinks
        CherryBlossom,
        RoseQuartz,
        MagentaPop,
        HotPink,
        Blush,
        Coral,
        Bubblegum,
        // Reds
        CrimsonRed,
        YouTubeRed,
        WineRed,
        RubyRed,
        Scarlet,
        // Neutrals
        Charcoal,
        Silver,
        Slate,
        Graphite,
        // Earth Tones
        Terracotta,
        Coffee,
        Mocha,
        Sand,
        Clay,
        // Pastels
        PastelPink,
        PastelBlue,
        PastelGreen,
        PastelYellow,
        PastelPurple,
        // Neon
        NeonGreen,
        NeonPink,
        NeonBlue,
        NeonOrange,
        Cyberpunk,
        Synthwave,
        // Special
        Ocean,
        Forest,
        Autumn,
        Winter,
        Spring,
        Summer,
        Twilight,
        Aurora,
        Candy,
        Rainbow
    )
    
    fun findByPrimaryColor(colorHex: String): ThemePalette? {
        return allPalettes.find { it.primary.toHexString() == colorHex }
    }
    
    fun findById(id: String): ThemePalette? {
        return allPalettes.find { it.id == id }
    }
}

private fun Color.toHexString(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalettePickerScreen(
    navController: NavController
) {
    val (customThemeColor, onCustomThemeColorChange) = rememberPreference(
        CustomThemeColorKey,
        defaultValue = ThemePalettes.Default.id
    )
    
    val selectedPalette = remember(customThemeColor) {
        ThemePalettes.findById(customThemeColor)
            ?: ThemePalettes.findByPrimaryColor(customThemeColor)
            ?: ThemePalettes.Default
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
            
            ThemePreviewCard(
                palette = selectedPalette,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
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
            
            ColorPaletteSelector(
                palettes = ThemePalettes.allPalettes,
                selectedPalette = selectedPalette,
                onPaletteSelected = { palette ->
                    onCustomThemeColorChange(palette.id)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            SelectedPaletteDetails(
                palette = selectedPalette,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

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
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
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

@Composable
private fun ColorPaletteSelector(
    palettes: List<ThemePalette>,
    selectedPalette: ThemePalette,
    onPaletteSelected: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = palettes.indexOf(selectedPalette)
    
    val totalDots = (palettes.size + 3) / 4
    
    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex / 4 }
    }
    
    var stableCurrentPage by rememberSaveable { mutableIntStateOf(0) }
    
    LaunchedEffect(currentPage) {
        kotlinx.coroutines.delay(50)
        stableCurrentPage = currentPage
    }
    
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(
                index = selectedIndex,
                scrollOffset = -100
            )
        }
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CarouselDotsIndicator(
            totalDots = totalDots,
            currentPage = stableCurrentPage,
            selectedColor = selectedPalette.primary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun CarouselDotsIndicator(
    totalDots: Int,
    currentPage: Int,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    val fixedDotContainerSize = 10.dp
    
    Row(
        modifier = modifier.height(fixedDotContainerSize),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            val isSelected = index == currentPage
            
            val dotSize by animateDpAsState(
                targetValue = if (isSelected) 8.dp else 4.dp,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "dotSize"
            )
            
            val dotColor by animateColorAsState(
                targetValue = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                animationSpec = tween(durationMillis = 200),
                label = "dotColor"
            )
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(fixedDotContainerSize),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

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
