/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */


package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import moe.koiverse.archivetune.LocalAnimationsDisabled

object SettingsDimensions {
    val GroupCardCornerRadius = 16.dp
    val QuickActionCardCornerRadius = 20.dp
    val IntegrationPillCornerRadius = 14.dp
    val BannerCardCornerRadius = 20.dp
    val HeroCardCornerRadius = 24.dp
    val RowIconCornerRadius = 12.dp

    val ScreenHorizontalPadding = 16.dp
    val CardInternalPadding = 16.dp
    val SectionSpacing = 14.dp
    val RowVerticalPadding = 14.dp
    val RowHorizontalPadding = 16.dp

    val RowIconSize = 36.dp
    val RowIconInnerSize = 20.dp
    val QuickActionIconSize = 40.dp
    val QuickActionIconInnerSize = 22.dp
    val HeroIconSize = 56.dp
    val HeroIconInnerSize = 30.dp
    val IntegrationIconSize = 28.dp
    val IntegrationIconInnerSize = 16.dp
    val BannerIconSize = 44.dp
    val BannerIconInnerSize = 22.dp
    val ChevronSize = 18.dp

    val DividerThickness = 0.5.dp
    val DividerStartIndent = 60.dp

    val SectionHeaderBottomPadding = 6.dp
    val SectionHeaderHorizontalPadding = 20.dp

    val QuickActionTileAspectRatio = 1.4f

    val CompactColumns = 2
    val MediumColumns = 4
    val ExpandedColumns = 4

    val MediumPaneLeftWeight = 0.42f
    val MediumPaneRightWeight = 0.58f
    val ExpandedListPaneWidth = 380.dp
}

object SettingsAnimations {
    val PressScale = 0.97f
    val TilePressScale = 0.94f
    val PillPressScale = 0.95f
    val IconPressRotation = 5f
    val PillPressLift = (-2).dp

    val EntranceFadeDuration = 300
    val EntranceSlideDuration = 350
    val StaggerDelayPerItem = 80
    val ExitFadeDuration = 200

    @Composable
    fun <T> pressSpring(): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) {
            snap()
        } else {
            spring(stiffness = Spring.StiffnessHigh)
        }

    @Composable
    fun <T> entranceSpring(): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) {
            snap()
        } else {
            spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = 0.85f,
            )
        }

    @Composable
    fun <T> exitTween(): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) {
            snap()
        } else {
            tween(durationMillis = ExitFadeDuration)
        }

    @Composable
    fun <T> fadeTween(durationMillis: Int): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) {
            snap()
        } else {
            tween(durationMillis = durationMillis)
        }

    @Composable
    fun <T> staggerTween(index: Int): FiniteAnimationSpec<T> =
        if (LocalAnimationsDisabled.current) {
            snap()
        } else {
            tween(
                durationMillis = EntranceSlideDuration,
                delayMillis = index * StaggerDelayPerItem,
            )
        }
}
