/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package moe.koiverse.archivetune.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.constants.ChipSortTypeKey
import moe.koiverse.archivetune.constants.DarkModeKey
import moe.koiverse.archivetune.constants.DefaultOpenTabKey
import moe.koiverse.archivetune.constants.DynamicThemeKey
import moe.koiverse.archivetune.constants.GridItemSize
import moe.koiverse.archivetune.constants.GridItemsSizeKey
import moe.koiverse.archivetune.constants.LibraryFilter
import moe.koiverse.archivetune.constants.LyricsClickKey
import moe.koiverse.archivetune.constants.LyricsScrollKey
import moe.koiverse.archivetune.constants.LyricsTextPositionKey
import moe.koiverse.archivetune.constants.PlayerDesignStyle
import moe.koiverse.archivetune.constants.PlayerDesignStyleKey
import moe.koiverse.archivetune.constants.UseNewMiniPlayerDesignKey
import moe.koiverse.archivetune.constants.PlayerBackgroundStyle
import moe.koiverse.archivetune.constants.PlayerBackgroundStyleKey
import moe.koiverse.archivetune.constants.PureBlackKey
import moe.koiverse.archivetune.constants.RandomThemeOnStartupKey
import moe.koiverse.archivetune.constants.UseSystemFontKey
import moe.koiverse.archivetune.constants.PlayerButtonsStyle
import moe.koiverse.archivetune.constants.PlayerButtonsStyleKey
import moe.koiverse.archivetune.constants.LyricsAnimationStyleKey
import moe.koiverse.archivetune.constants.LyricsAnimationStyle
import moe.koiverse.archivetune.constants.LyricsTextSizeKey
import moe.koiverse.archivetune.constants.LyricsLineSpacingKey
import moe.koiverse.archivetune.constants.SliderStyle
import moe.koiverse.archivetune.constants.SliderStyleKey
import moe.koiverse.archivetune.constants.SlimNavBarKey
import moe.koiverse.archivetune.constants.ShowLikedPlaylistKey
import moe.koiverse.archivetune.constants.ShowDownloadedPlaylistKey
import moe.koiverse.archivetune.constants.ShowTopPlaylistKey
import moe.koiverse.archivetune.constants.ShowCachedPlaylistKey
import moe.koiverse.archivetune.constants.ShowTagsInLibraryKey
import moe.koiverse.archivetune.constants.SwipeThumbnailKey
import moe.koiverse.archivetune.constants.SwipeSensitivityKey
import moe.koiverse.archivetune.constants.SwipeToSongKey
import moe.koiverse.archivetune.constants.HidePlayerThumbnailKey
import moe.koiverse.archivetune.constants.ArchiveTuneCanvasKey
import moe.koiverse.archivetune.constants.ThumbnailCornerRadiusKey
import moe.koiverse.archivetune.constants.CropThumbnailToSquareKey
import moe.koiverse.archivetune.constants.DisableBlurKey
import moe.koiverse.archivetune.constants.UseLyricsV2Key
import moe.koiverse.archivetune.ui.component.DefaultDialog
import moe.koiverse.archivetune.ui.component.EnumListPreference
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.component.ListPreference
import moe.koiverse.archivetune.ui.component.PlayerSliderTrack
import moe.koiverse.archivetune.ui.component.PreferenceEntry
import moe.koiverse.archivetune.ui.component.PreferenceGroupTitle
import moe.koiverse.archivetune.ui.component.SwitchPreference
import moe.koiverse.archivetune.ui.component.ThumbnailCornerRadiusSelectorButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.utils.rememberEnumPreference
import moe.koiverse.archivetune.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (randomThemeOnStartup, onRandomThemeOnStartupChange) = rememberPreference(
        RandomThemeOnStartupKey,
        defaultValue = false
    )
    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )
    val (playerDesignStyle, onPlayerDesignStyleChange) = rememberEnumPreference(
        PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.V2
    )
    val (useNewMiniPlayerDesign, onUseNewMiniPlayerDesignChange) = rememberPreference(
        UseNewMiniPlayerDesignKey,
        defaultValue = true
    )
    val (useNewLibraryDesign, onUseNewLibraryDesignChange) = rememberPreference(
        key = moe.koiverse.archivetune.constants.UseNewLibraryDesignKey,
        defaultValue = false
    )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(
        HidePlayerThumbnailKey,
        defaultValue = false
    )
    val (archiveTuneCanvasEnabled, onArchiveTuneCanvasEnabledChange) = rememberPreference(
        ArchiveTuneCanvasKey,
        defaultValue = false
    )
    val (thumbnailCornerRadius, onThumbnailCornerRadiusChange) = rememberPreference(
        key = ThumbnailCornerRadiusKey,
        defaultValue = 16f // default dp
    )
    val (cropThumbnailToSquare, onCropThumbnailToSquareChange) = rememberPreference(
        CropThumbnailToSquareKey,
        defaultValue = false
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.DEFAULT,
        )
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (disableBlur, onDisableBlurChange) = rememberPreference(DisableBlurKey, defaultValue = true)
    val (useSystemFont, onUseSystemFontChange) = rememberPreference(UseSystemFontKey, defaultValue = false)
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.LEFT
    )
    val (lyricsAnimation, onLyricsAnimationChange) = rememberEnumPreference<LyricsAnimationStyle>(
    key = LyricsAnimationStyleKey,
    defaultValue = LyricsAnimationStyle.APPLE
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 26f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)
    val (useLyricsV2, onUseLyricsV2Change) = rememberPreference(UseLyricsV2Key, defaultValue = false)

    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.DEFAULT
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (swipeSensitivity, onSwipeSensitivityChange) = rememberPreference(
        SwipeSensitivityKey,
        defaultValue = 0.73f
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.SMALL
    )

    val (slimNav, onSlimNavChange) = rememberPreference(
        SlimNavBarKey,
        defaultValue = false
    )

    val (swipeToSong, onSwipeToSongChange) = rememberPreference(
        SwipeToSongKey,
        defaultValue = false
    )

    val (showLikedPlaylist, onShowLikedPlaylistChange) = rememberPreference(
        ShowLikedPlaylistKey,
        defaultValue = true
    )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) = rememberPreference(
        ShowDownloadedPlaylistKey,
        defaultValue = true
    )
    val (showTopPlaylist, onShowTopPlaylistChange) = rememberPreference(
        ShowTopPlaylistKey,
        defaultValue = true
    )
    val (showCachedPlaylist, onShowCachedPlaylistChange) = rememberPreference(
        ShowCachedPlaylistKey,
        defaultValue = true
    )
    val (showTagsInLibrary, onShowTagsInLibraryChange) = rememberPreference(
        ShowTagsInLibraryKey,
        defaultValue = true
    )

    val availableBackgroundStyles = PlayerBackgroundStyle.entries.filter {
        it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
        }

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.DEFAULT)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.default_),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SQUIGGLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SQUIGGLY)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    SquigglySlider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.squiggly),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SLIM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SLIM)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {}
                                )
                            }
                    )

                    Text(
                        text = stringResource(R.string.slim),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(
            title = stringResource(R.string.theme),
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_dynamic_theme)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            checked = dynamicTheme,
            onCheckedChange = onDynamicThemeChange,
        )

        AnimatedVisibility(visible = !dynamicTheme || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            SwitchPreference(
                title = { Text(stringResource(R.string.random_theme_on_startup)) },
                description = stringResource(R.string.random_theme_on_startup_desc),
                icon = { Icon(painterResource(R.drawable.shuffle), null) },
                checked = randomThemeOnStartup,
                onCheckedChange = onRandomThemeOnStartupChange,
            )
        }

        AnimatedVisibility(visible = !dynamicTheme || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.color_palette)) },
                description = stringResource(R.string.customize_theme_colors),
                icon = { Icon(painterResource(R.drawable.format_paint), null) },
                onClick = { navController.navigate("settings/appearance/palette_picker") }
            )
        }

        EnumListPreference(
            title = { Text(stringResource(R.string.dark_theme)) },
            icon = { Icon(painterResource(R.drawable.dark_mode), null) },
            selectedValue = darkMode,
            onValueSelected = onDarkModeChange,
            valueText = {
                when (it) {
                    DarkMode.ON -> stringResource(R.string.dark_theme_on)
                    DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                    DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                }
            },
        )

        AnimatedVisibility(useDarkTheme) {
            SwitchPreference(
                title = { Text(stringResource(R.string.pure_black)) },
                icon = { Icon(painterResource(R.drawable.contrast), null) },
                checked = pureBlack,
                onCheckedChange = onPureBlackChange,
            )
        }

        SwitchPreference(
            title = { Text(stringResource(R.string.disable_blur)) },
            description = stringResource(R.string.disable_blur_desc),
            icon = { Icon(painterResource(R.drawable.blur_off), null) },
            checked = disableBlur,
            onCheckedChange = onDisableBlurChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.use_system_font)) },
            description = stringResource(R.string.use_system_font_desc),
            icon = { Icon(painterResource(R.drawable.text_fields), null) },
            checked = useSystemFont,
            onCheckedChange = onUseSystemFontChange,
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.player),
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.player_design_style)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            selectedValue = playerDesignStyle,
            onValueSelected = onPlayerDesignStyleChange,
            valueText = {
                when (it) {
                    PlayerDesignStyle.V1 -> stringResource(R.string.player_design_v1)
                    PlayerDesignStyle.V2 -> stringResource(R.string.player_design_v2)
                    PlayerDesignStyle.V3 -> stringResource(R.string.player_design_v3)
                    PlayerDesignStyle.V4 -> stringResource(R.string.player_design_v4)
                    PlayerDesignStyle.V5 -> stringResource(R.string.player_design_v5)
                }
            },
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.new_mini_player_design)) },
            icon = { Icon(painterResource(R.drawable.nav_bar), null) },
            checked = useNewMiniPlayerDesign,
            onCheckedChange = onUseNewMiniPlayerDesignChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.new_library_design)) },
            description = stringResource(R.string.new_library_design_description),
            icon = { Icon(painterResource(R.drawable.grid_view), null) },
            checked = useNewLibraryDesign,
            onCheckedChange = onUseNewLibraryDesignChange,
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.player_background_style)) },
            icon = { Icon(painterResource(R.drawable.gradient), null) },
            selectedValue = playerBackground,
            onValueSelected = onPlayerBackgroundChange,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                        PlayerBackgroundStyle.CUSTOM -> stringResource(R.string.custom)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    PlayerBackgroundStyle.COLORING -> stringResource(R.string.coloring)
                    PlayerBackgroundStyle.BLUR_GRADIENT -> stringResource(R.string.blur_gradient)
                    PlayerBackgroundStyle.GLOW -> stringResource(R.string.glow)
                    PlayerBackgroundStyle.GLOW_ANIMATED -> "Glow Animated"
                }
            },
        )

        // When custom background is selected, show a direct link to customize it
        if (playerBackground == PlayerBackgroundStyle.CUSTOM) {
            PreferenceEntry(
                title = { Text(stringResource(R.string.customized_background)) },
                icon = { Icon(painterResource(R.drawable.image), null) },
                onClick = { navController.navigate("customize_background") }
            )
        }

        SwitchPreference(
            title = { Text(stringResource(R.string.hide_player_thumbnail)) },
            description = stringResource(R.string.hide_player_thumbnail_desc),
            icon = { Icon(painterResource(R.drawable.hide_image), null) },
            checked = hidePlayerThumbnail,
            onCheckedChange = onHidePlayerThumbnailChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.archivetune_canvas)) },
            description = stringResource(R.string.archivetune_canvas_desc),
            icon = { Icon(painterResource(R.drawable.motion_photos_on), null) },
            checked = archiveTuneCanvasEnabled,
            onCheckedChange = onArchiveTuneCanvasEnabledChange
        )
      

        ThumbnailCornerRadiusSelectorButton(
            modifier = Modifier.padding(16.dp),
            onRadiusSelected = { selectedRadius ->
                Timber.tag("Thumbnail").d("Radius Selector: $selectedRadius")
            }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.crop_thumbnail_to_square)) },
            description = stringResource(R.string.crop_thumbnail_to_square_desc),
            icon = { Icon(painterResource(R.drawable.image), null) },
            checked = cropThumbnailToSquare,
            onCheckedChange = onCropThumbnailToSquareChange
        )


        EnumListPreference(
            title = { Text(stringResource(R.string.player_buttons_style)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            selectedValue = playerButtonsStyle,
            onValueSelected = onPlayerButtonsStyleChange,
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
                }
            },
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.player_slider_style)) },
            description =
                when (sliderStyle) {
                    SliderStyle.DEFAULT -> stringResource(R.string.default_)
                    SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
                    SliderStyle.SLIM -> stringResource(R.string.slim)
                },
            icon = { Icon(painterResource(R.drawable.sliders), null) },
            onClick = {
                showSliderOptionDialog = true
            },
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
            icon = { Icon(painterResource(R.drawable.swipe), null) },
            checked = swipeThumbnail,
            onCheckedChange = onSwipeThumbnailChange,
        )

        AnimatedVisibility(swipeThumbnail) {
            var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }
            
            if (showSensitivityDialog) {
                var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }
                
                DefaultDialog(
                    onDismiss = { 
                        tempSensitivity = swipeSensitivity
                        showSensitivityDialog = false 
                    },
                    buttons = {
                        TextButton(
                            onClick = { 
                                tempSensitivity = 0.73f
                            }
                        ) {
                            Text(stringResource(R.string.reset))
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        TextButton(
                            onClick = { 
                                tempSensitivity = swipeSensitivity
                                showSensitivityDialog = false 
                            }
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                        TextButton(
                            onClick = { 
                                onSwipeSensitivityChange(tempSensitivity)
                                showSensitivityDialog = false 
                            }
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.swipe_sensitivity),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
    
                        Text(
                            text = stringResource(R.string.sensitivity_percentage, (tempSensitivity * 100).roundToInt()),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
    
                        Slider(
                            value = tempSensitivity,
                            onValueChange = { tempSensitivity = it },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            PreferenceEntry(
                title = { Text(stringResource(R.string.swipe_sensitivity)) },
                description = stringResource(R.string.sensitivity_percentage, (swipeSensitivity * 100).roundToInt()),
                icon = { Icon(painterResource(R.drawable.tune), null) },
                onClick = { showSensitivityDialog = true }
            )
        }

        PreferenceGroupTitle(
            title = stringResource(R.string.lyrics),
        )

        SwitchPreference(
            title = { Text("Lyrics V2 (Experimental)") },
            description = "Use the new fluid word-synced lyrics engine",
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = useLyricsV2,
            onCheckedChange = onUseLyricsV2Change,
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.lyrics_text_position)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            selectedValue = lyricsPosition,
            onValueSelected = onLyricsPositionChange,
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            },
        )

        EnumListPreference(
          title = { Text(stringResource(R.string.lyrics_animation_style)) },
          icon = { Icon(painterResource(R.drawable.animation), null) },
          selectedValue = lyricsAnimation,
          onValueSelected = onLyricsAnimationChange,
          valueText = {
              when (it) {
                  LyricsAnimationStyle.NONE -> stringResource(R.string.none)
                  LyricsAnimationStyle.FADE -> stringResource(R.string.fade)
                  LyricsAnimationStyle.GLOW -> stringResource(R.string.glow)
                  LyricsAnimationStyle.SLIDE -> stringResource(R.string.slide)
                  LyricsAnimationStyle.KARAOKE -> stringResource(R.string.karaoke)
                  LyricsAnimationStyle.APPLE -> stringResource(R.string.apple_music_style)
              }
          }
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_click_change)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsClick,
            onCheckedChange = onLyricsClickChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
            icon = { Icon(painterResource(R.drawable.lyrics), null) },
            checked = lyricsScroll,
            onCheckedChange = onLyricsScrollChange,
        )

        var showLyricsTextSizeDialog by rememberSaveable { mutableStateOf(false) }
        
        if (showLyricsTextSizeDialog) {
            var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }
            
            DefaultDialog(
                onDismiss = { 
                    tempTextSize = lyricsTextSize
                    showLyricsTextSizeDialog = false 
                },
                buttons = {
                    TextButton(
                        onClick = { 
                            tempTextSize = 24f
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    TextButton(
                        onClick = { 
                            tempTextSize = lyricsTextSize
                            showLyricsTextSizeDialog = false 
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = { 
                            onLyricsTextSizeChange(tempTextSize)
                            showLyricsTextSizeDialog = false 
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_text_size),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "${tempTextSize.roundToInt()} sp",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = tempTextSize,
                        onValueChange = { tempTextSize = it },
                        valueRange = 16f..36f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        PreferenceEntry(
            title = { Text(stringResource(R.string.lyrics_text_size)) },
            description = "${lyricsTextSize.roundToInt()} sp",
            icon = { Icon(painterResource(R.drawable.text_fields), null) },
            onClick = { showLyricsTextSizeDialog = true }
        )
        
        var showLyricsLineSpacingDialog by rememberSaveable { mutableStateOf(false) }
        
        if (showLyricsLineSpacingDialog) {
            var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }
            
            DefaultDialog(
                onDismiss = { 
                    tempLineSpacing = lyricsLineSpacing
                    showLyricsLineSpacingDialog = false 
                },
                buttons = {
                    TextButton(
                        onClick = { 
                            tempLineSpacing = 1.3f
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    TextButton(
                        onClick = { 
                            tempLineSpacing = lyricsLineSpacing
                            showLyricsLineSpacingDialog = false 
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = { 
                            onLyricsLineSpacingChange(tempLineSpacing)
                            showLyricsLineSpacingDialog = false 
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_line_spacing),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "${String.format("%.1f", tempLineSpacing)}x",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Slider(
                        value = tempLineSpacing,
                        onValueChange = { tempLineSpacing = it },
                        valueRange = 1.0f..2.0f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        PreferenceEntry(
            title = { Text(stringResource(R.string.lyrics_line_spacing)) },
            description = "${String.format("%.1f", lyricsLineSpacing)}x",
            icon = { Icon(painterResource(R.drawable.text_fields), null) },
            onClick = { showLyricsLineSpacingDialog = true }
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.misc),
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.default_open_tab)) },
            icon = { Icon(painterResource(R.drawable.nav_bar), null) },
            selectedValue = defaultOpenTab,
            onValueSelected = onDefaultOpenTabChange,
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                }
            },
        )

        ListPreference(
            title = { Text(stringResource(R.string.default_lib_chips)) },
            icon = { Icon(painterResource(R.drawable.tab), null) },
            selectedValue = defaultChip,
            values = listOf(
                LibraryFilter.LIBRARY, LibraryFilter.PLAYLISTS, LibraryFilter.SONGS,
                LibraryFilter.ALBUMS, LibraryFilter.ARTISTS
            ),
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                }
            },
            onValueSelected = onDefaultChipChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.show_tags_in_library)) },
            description = stringResource(R.string.show_tags_in_library_desc),
            icon = { Icon(painterResource(R.drawable.filter_alt), null) },
            checked = showTagsInLibrary,
            onCheckedChange = onShowTagsInLibraryChange,
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.swipe_song_to_add)) },
            icon = { Icon(painterResource(R.drawable.swipe), null) },
            checked = swipeToSong,
            onCheckedChange = onSwipeToSongChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.slim_navbar)) },
            icon = { Icon(painterResource(R.drawable.nav_bar), null) },
            checked = slimNav,
            onCheckedChange = onSlimNavChange
        )

        EnumListPreference(
            title = { Text(stringResource(R.string.grid_cell_size)) },
            icon = { Icon(painterResource(R.drawable.grid_view), null) },
            selectedValue = gridItemSize,
            onValueSelected = onGridItemSizeChange,
            valueText = {
                when (it) {
                    GridItemSize.BIG -> stringResource(R.string.big)
                    GridItemSize.SMALL -> stringResource(R.string.small)
                }
            },
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.auto_playlists)
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.show_liked_playlist)) },
            icon = { Icon(painterResource(R.drawable.favorite), null) },
            checked = showLikedPlaylist,
            onCheckedChange = onShowLikedPlaylistChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.show_downloaded_playlist)) },
            icon = { Icon(painterResource(R.drawable.offline), null) },
            checked = showDownloadedPlaylist,
            onCheckedChange = onShowDownloadedPlaylistChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.show_top_playlist)) },
            icon = { Icon(painterResource(R.drawable.trending_up), null) },
            checked = showTopPlaylist,
            onCheckedChange = onShowTopPlaylistChange
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.show_cached_playlist)) },
            icon = { Icon(painterResource(R.drawable.cached), null) },
            checked = showCachedPlaylist,
            onCheckedChange = onShowCachedPlaylistChange
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
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
        }
    )
}

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}
