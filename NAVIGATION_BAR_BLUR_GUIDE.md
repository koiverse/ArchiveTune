# Navigation Bar Blur Implementation Guide

## Overview
This guide explains how to add a blur effect to top navigation bars across all pages in ArchiveTune, replacing the current opaque/transparent behavior.

## Current Status
✅ **Completed Improvements:**
1. Fixed lyrics SLIDE animation to match word duration dynamically
2. Added romanization sync with word-by-word lyrics animation
3. Added gradient background to LibraryPlaylistsScreen
4. Performance optimization (gradients use cached colors with remember)

## Navigation Bar Blur Implementation

### Approach: Using Modifier.blur() with Background Scrim

Since Material 3 TopAppBar doesn't natively support blur, we need to create a custom composable or modify the TopAppBar styling.

### Option 1: Custom Blurred TopAppBar Component

Create a new file: `app/src/main/kotlin/moe/koiverse/archivetune/ui/component/BlurredTopAppBar.kt`

```kotlin
package moe.koiverse.archivetune.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlurredTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    blurRadius: Float = 12f,
    scrimAlpha: Float = 0.7f
) {
    Box(modifier = modifier) {
        // Blurred background layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = scrimAlpha))
                .blur(radius = blurRadius.dp)
        )
        
        // TopAppBar with transparent background
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}
```

### Option 2: Modify Existing Screens

For screens like `HomeScreen.kt`, `LibraryScreen.kt`, etc., replace the existing TopAppBar with the blurred version:

**Before:**
```kotlin
TopAppBar(
    title = { Text("Home") },
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
)
```

**After:**
```kotlin
BlurredTopAppBar(
    title = { Text("Home") },
    blurRadius = 12f,
    scrimAlpha = 0.7f
)
```

### Option 3: System-Wide Theme Modification (Recommended)

Modify the theme to apply blur globally. Add to `Theme.kt`:

```kotlin
import androidx.compose.ui.draw.blur
import androidx.compose.material3.TopAppBarDefaults

@Composable
fun ArchiveTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalTopAppBarColors provides TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface.copy(alpha = 0.7f),
                    scrolledContainerColor = colorScheme.surface.copy(alpha = 0.85f)
                )
            ) {
                content()
            }
        }
    )
}
```

### Files to Modify (for Option 2)

1. `HomeScreen.kt` - Replace TopAppBar if present
2. `LibraryScreen.kt` - Replace TopAppBar
3. `AlbumScreen.kt` - Replace TopAppBar with scrollBehavior
4. `ArtistScreen.kt` - Replace TopAppBar with scrollBehavior
5. `PlaylistScreen.kt` variants - Replace TopAppBar
6. `SettingsScreen.kt` - Replace TopAppBar
7. All other screens with top bars

### Testing Considerations

1. **Performance**: Blur can be expensive on lower-end devices. Consider:
   - Using lower blur radius (8-10dp) for better performance
   - Disabling blur on devices with API < 31 (Android 12)
   - Add a setting to disable blur

2. **Visual Consistency**: Ensure blur works well with:
   - Different background colors
   - Gradient backgrounds (like the new LibraryPlaylistsScreen)
   - Light and dark themes

3. **Scroll Behavior**: Test with `TopAppBarScrollBehavior` to ensure smooth transitions

### Performance Optimization

```kotlin
// Check API level before applying blur
val shouldBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val blurModifier = if (shouldBlur) Modifier.blur(12.dp) else Modifier

Box(
    modifier = Modifier
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
        .then(blurModifier)
)
```

## Summary of All Fixes Applied

### 1. ✅ Lyrics SLIDE Animation Speed Fix
**File**: `Lyrics.kt` (lines 694-722)
- Animation now responds to word duration
- Shorter words (< 300ms): Linear progression
- Medium words (300-600ms): Slight ease (0.8 power)
- Longer words (> 600ms): More ease (0.7 power)
- Results in smooth, natural-feeling karaoke effect

### 2. ✅ Romanization Sync with Lyrics
**File**: `Lyrics.kt` (lines 985-1078)
- Romanized text now syncs word-by-word with main lyrics
- Maps romanized words to main word timings
- Applies same animation styles (SLIDE/GLOW)
- Active words brighten, inactive words dim
- Falls back to static display when animations are off

### 3. ✅ Library Playlists Gradient Background
**File**: `LibraryPlaylistsScreen.kt`
- Added 5-blob mesh gradient matching HomeScreen style
- Extracts colors from first playlist with thumbnails
- Scroll-based fade (900dp fade distance)
- Works with both LIST and GRID view types
- Uses cached colors for performance

### 4. ✅ Performance Optimizations
- Gradient colors cached with `remember`
- Color extraction runs once per playlist change
- Scroll-based alpha uses `derivedStateOf` for efficiency
- Bitmap processing on Dispatchers.Default
- No unnecessary recompositions

## Next Steps for Navigation Bar Blur

1. Choose implementation approach (Option 1, 2, or 3)
2. Create `BlurredTopAppBar.kt` component if using Option 1
3. Update all screen files to use blurred variant
4. Test on different devices and API levels
5. Add user preference to enable/disable blur
6. Profile performance on lower-end devices

## Notes
- The blur effect requires Android 12+ (API 31) for optimal results
- Consider adding a fallback semi-transparent style for older devices
- Monitor frame rates during scroll with blur enabled
