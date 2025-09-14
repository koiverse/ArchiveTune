package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import moe.koiverse.archivetune.BuildConfig
import moe.koiverse.archivetune.LocalPlayerAwareWindowInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import coil.compose.AsyncImage
import moe.koiverse.archivetune.R
import moe.koiverse.archivetune.ui.component.IconButton
import moe.koiverse.archivetune.ui.utils.backToMain

data class TeamMember(
    val avatarUrl: String,
    val name: String,
    val position: String,
    val profileUrl: String? = null
)

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
            profileUrl = "https://github.com/koiverse"
        )
    )


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        Spacer(Modifier.height(4.dp))

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
                onClick = { uriHandler.openUri("https://prplmoe.me") },
            ) {
                Icon(
                    painter = painterResource(R.drawable.website),
                    contentDescription = null
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Team",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            teamMembers.forEach { member ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = member.profileUrl != null) {
                            member.profileUrl?.let { uriHandler.openUri(it) }
                        }
                        .padding(vertical = 4.dp)
                ) {
                    AsyncImage(
                        model = member.avatarUrl,
                        contentDescription = member.name,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = member.position,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }

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
        }
    )
}
