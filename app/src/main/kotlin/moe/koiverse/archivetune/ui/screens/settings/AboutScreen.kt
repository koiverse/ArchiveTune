package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.material.ripple.rememberRipple

data class TeamMember(
    val avatarUrl: String,
    val name: String,
    val position: String,
    val profileUrl: String? = null,
    val github: String? = null,
    val website: String? = null,
    val discord: String? = null,
    val hasEasterEgg: Boolean = false,
    val easterEggName: String? = null,
    val easterEggPosition: String? = null,
    val easterEggAvatarUrl: String? = null
)

@Composable
fun OutlinedIconChip(
    iconRes: Int,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 6.dp
        )
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OutlinedIconChipMembers(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current

    val teamMembers = listOf(
        TeamMember(
            avatarUrl = "https://avatar-api.koiisannn.cloud/discord/avatar/886971572668219392",
            name = "Koiverse",
            position = "always on mode UwU",
            profileUrl = "https://github.com/koiverse",
            github = "https://github.com/koiverse",
            website = "https://prplmoe.me", // If blank, hide OutlinedIconChip for website
            discord = "https://discord.com/users/886971572668219392"
        ),
        TeamMember(
            avatarUrl = "https://avatars.githubusercontent.com/u/93458424?v=4",
            name = "WTTexe",
            position = "Word Synced Lyrics, Gradients and UI Changes for the better!",
            profileUrl = "https://github.com/Windowstechtips",
            github = "https://github.com/Windowstechtips",
            website = null,
            discord = "https://discord.com/users/840839409640800258",
            hasEasterEgg = true,
            easterEggName = "Hououin Kyouma",
            easterEggPosition = "El psy congroo",
            easterEggAvatarUrl = "https://media.discordapp.net/attachments/1213837772599726110/1437508743037456404/images.jpeg?ex=69137fd7&is=69122e57&hm=8c5a1605e480765fe4798035f89783cd0d40d188729832f8669007eb868c3935&=&format=webp"
        ),
        TeamMember(
            avatarUrl = "https://avatars.githubusercontent.com/u/80542861?v=4",
            name = "MO AGAMY",
            position = "Original Developer",
            profileUrl = "https://github.com/mostafaalagamy",
            github = "https://github.com/mostafaalagamy",
            website = null,
            discord = null
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
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
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .heightIn(max = 16.dp)
            )

            Image(
                painter = painterResource(R.drawable.about_splash),
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { },
            )

            Row(
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = "ArchiveTune",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            shape = CircleShape,
                        )
                        .padding(
                            horizontal = 6.dp,
                            vertical = 2.dp,
                        ),
                )

                Spacer(Modifier.width(4.dp))

                if (BuildConfig.DEBUG) {
                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = "DEBUG",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                shape = CircleShape,
                            )
                            .padding(
                                horizontal = 6.dp,
                                vertical = 2.dp,
                            ),
                    )
                } else {
                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = BuildConfig.ARCHITECTURE.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                shape = CircleShape,
                            )
                            .padding(
                                horizontal = 6.dp,
                                vertical = 2.dp,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Koiverse",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(Modifier.height(8.dp))

            Row {
                IconButton(
                    onClick = { uriHandler.openUri("https://github.com/koiverse/archivetune") },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.github),
                        contentDescription = null
                    )
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = { uriHandler.openUri("https://archivetune.prplmoe.me") },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.website),
                        contentDescription = null
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                teamMembers.forEach { member ->
                    var clickCount by remember { mutableStateOf(0) }
                    var isShaking by remember { mutableStateOf(false) }
                    var isGlitching by remember { mutableStateOf(false) }
                    var showEasterEgg by remember { mutableStateOf(false) }
                    var glitchOffsetX by remember { mutableStateOf(0f) }
                    var glitchOffsetY by remember { mutableStateOf(0f) }
                    var glitchAlpha by remember { mutableStateOf(1f) }
                    var chromaticAberration by remember { mutableStateOf(0f) }
                    
                    // Word-by-word animation states for "El psy congroo"
                    var showWord1 by remember { mutableStateOf(false) } // "El"
                    var showWord2 by remember { mutableStateOf(false) } // "psy"
                    var showWord3 by remember { mutableStateOf(false) } // "congroo"
                    
                    // Shake effect for clicks 1-3
                    LaunchedEffect(isShaking) {
                        if (isShaking) {
                            val intensity = clickCount * 5f // Increasing intensity
                            repeat(10) {
                                glitchOffsetX = Random.nextFloat() * intensity - intensity / 2
                                glitchOffsetY = Random.nextFloat() * intensity - intensity / 2
                                chromaticAberration = Random.nextFloat() * clickCount * 2f
                                delay(50)
                            }
                            // Reset
                            glitchOffsetX = 0f
                            glitchOffsetY = 0f
                            chromaticAberration = 0f
                            isShaking = false
                        }
                    }
                    
                    // Final glitch effect on 4th click
                    LaunchedEffect(isGlitching) {
                        if (isGlitching) {
                            // Intense glitch effect for 800ms
                            repeat(20) {
                                glitchOffsetX = Random.nextFloat() * 30f - 15f
                                glitchOffsetY = Random.nextFloat() * 30f - 15f
                                glitchAlpha = Random.nextFloat() * 0.5f + 0.5f
                                chromaticAberration = Random.nextFloat() * 8f
                                delay(40)
                            }
                            // Reset and show easter egg
                            glitchOffsetX = 0f
                            glitchOffsetY = 0f
                            glitchAlpha = 1f
                            chromaticAberration = 0f
                            showEasterEgg = true
                            isGlitching = false
                        }
                    }
                    
                    // Word-by-word animation sequence
                    LaunchedEffect(showEasterEgg) {
                        if (showEasterEgg && member.hasEasterEgg) {
                            showWord1 = false
                            showWord2 = false
                            showWord3 = false
                            delay(100)
                            showWord1 = true
                            delay(400)
                            showWord2 = true
                            delay(400)
                            showWord3 = true
                        } else {
                            showWord1 = false
                            showWord2 = false
                            showWord3 = false
                        }
                    }
                    
                    val displayName = if (showEasterEgg && member.hasEasterEgg) member.easterEggName ?: member.name else member.name
                    val displayPosition = if (showEasterEgg && member.hasEasterEgg) member.easterEggPosition ?: member.position else member.position
                    val displayAvatarUrl = if (showEasterEgg && member.hasEasterEgg && member.easterEggAvatarUrl != null) member.easterEggAvatarUrl else member.avatarUrl
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .graphicsLayer {
                                if (isGlitching || isShaking) {
                                    translationX = glitchOffsetX
                                    translationY = glitchOffsetY
                                    alpha = glitchAlpha
                                }
                            }
                            .drawWithContent {
                                // Chromatic aberration effect
                                if (chromaticAberration > 0f) {
                                    // Red channel
                                    translate(left = -chromaticAberration, top = 0f) {
                                        this@drawWithContent.drawContent()
                                    }
                                    // Green channel (normal position)
                                    drawContent()
                                    // Blue channel
                                    translate(left = chromaticAberration, top = 0f) {
                                        this@drawWithContent.drawContent()
                                    }
                                } else {
                                    drawContent()
                                }
                            }
                            .clickable(
                                enabled = member.profileUrl != null || member.hasEasterEgg,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(
                                    bounded = true,
                                    radius = 300.dp
                                )
                            ) {
                                if (member.hasEasterEgg && !showEasterEgg) {
                                    clickCount++
                                    if (clickCount >= 4) {
                                        // Trigger full easter egg on 4th click
                                        isGlitching = true
                                        clickCount = 0 // Reset for next time
                                    } else {
                                        // Just shake for clicks 1-3
                                        isShaking = true
                                    }
                                } else if (showEasterEgg && member.hasEasterEgg) {
                                    // Reset everything
                                    showEasterEgg = false
                                    clickCount = 0
                                } else {
                                    member.profileUrl?.let { uriHandler.openUri(it) }
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = displayAvatarUrl,
                                contentDescription = displayName,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                Spacer(Modifier.height(2.dp))

                                // Easter egg position with word-by-word fade-in
                                if (showEasterEgg && member.hasEasterEgg) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        AnimatedVisibility(
                                            visible = showWord1,
                                            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(300))
                                        ) {
                                            Text(
                                                text = "El",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        AnimatedVisibility(
                                            visible = showWord2,
                                            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(300))
                                        ) {
                                            Text(
                                                text = "psy",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        AnimatedVisibility(
                                            visible = showWord3,
                                            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(300))
                                        ) {
                                            Text(
                                                text = "congroo",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = displayPosition,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    member.github?.let {
                                        OutlinedIconChipMembers(
                                            iconRes = R.drawable.github,
                                            onClick = { uriHandler.openUri(it) },
                                            contentDescription = "GitHub"
                                        )
                                    }

                                    member.website?.takeIf { it.isNotBlank() }?.let {
                                        OutlinedIconChipMembers(
                                            iconRes = R.drawable.website,
                                            onClick = { uriHandler.openUri(it) },
                                            contentDescription = "Website"
                                        )
                                    }

                                    member.discord?.let {
                                        OutlinedIconChipMembers(
                                            iconRes = R.drawable.alternate_email,
                                            onClick = { uriHandler.openUri(it) },
                                            contentDescription = "Discord"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}
