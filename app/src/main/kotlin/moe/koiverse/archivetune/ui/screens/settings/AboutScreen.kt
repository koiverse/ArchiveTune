/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.datastore.preferences.core.edit
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import moe.koiverse.archivetune.constants.GitHubContributorsEtagKey
import moe.koiverse.archivetune.constants.GitHubContributorsJsonKey
import moe.koiverse.archivetune.constants.GitHubContributorsLastCheckedAtKey
import moe.koiverse.archivetune.utils.dataStore
import moe.koiverse.archivetune.utils.getAsync
import org.json.JSONArray

data class TeamMember(
    val avatarUrl: String,
    val name: String,
    val position: String,
    val profileUrl: String? = null,
    val github: String? = null,
    val website: String? = null,
    val discord: String? = null

)

private data class GitHubContributor(
    val login: String,
    val avatarUrl: String,
    val profileUrl: String,
)

private sealed interface ContributorsState {
    data object Loading : ContributorsState
    data class Loaded(val contributors: List<GitHubContributor>) : ContributorsState
    data object Error : ContributorsState
}

private const val ContributorsCacheCheckIntervalMs: Long = 24 * 60 * 60 * 1000L

private fun parseContributorsJson(
    json: String,
): List<GitHubContributor> {
    val jsonArray = JSONArray(json)
    val contributors = ArrayList<GitHubContributor>(jsonArray.length())
    for (i in 0 until jsonArray.length()) {
        val item = jsonArray.getJSONObject(i)
        val login = item.optString("login", "")
        val type = item.optString("type", "")
        val avatarUrl = item.optString("avatar_url", "")
        val profileUrl = item.optString("html_url", "")
        val isBot =
            type.equals("Bot", ignoreCase = true) ||
                login.lowercase().endsWith("[bot]")

        if (!isBot && login.isNotBlank() && avatarUrl.isNotBlank()) {
            contributors.add(
                GitHubContributor(
                    login = login,
                    avatarUrl = avatarUrl,
                    profileUrl = profileUrl,
                )
            )
        }
    }
    return contributors
}

private data class ContributorsNetworkResult(
    val status: HttpStatusCode,
    val body: String?,
    val etag: String?,
)

private suspend fun fetchRepoContributorsNetwork(
    client: HttpClient,
    owner: String,
    repo: String,
    perPage: Int = 100,
    cachedEtag: String?,
): ContributorsNetworkResult {
    val response: HttpResponse =
        client.get("https://api.github.com/repos/$owner/$repo/contributors?per_page=$perPage") {
            headers {
                append("Accept", "application/vnd.github+json")
                append("User-Agent", "ArchiveTune")
                if (!cachedEtag.isNullOrBlank()) {
                    append("If-None-Match", cachedEtag)
                }
            }
        }
    val etag = response.headers["ETag"]
    return when (response.status) {
        HttpStatusCode.NotModified ->
            ContributorsNetworkResult(
                status = response.status,
                body = null,
                etag = cachedEtag ?: etag,
            )

        else ->
            ContributorsNetworkResult(
                status = response.status,
                body = response.bodyAsText(),
                etag = etag,
            )
    }
}

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
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val httpClient = remember { HttpClient() }
    DisposableEffect(Unit) {
        onDispose { httpClient.close() }
    }
    var contributorsState by remember { mutableStateOf<ContributorsState>(ContributorsState.Loading) }
    LaunchedEffect(Unit) {
        val cachedJson = context.dataStore.getAsync(GitHubContributorsJsonKey)
        val cachedEtag = context.dataStore.getAsync(GitHubContributorsEtagKey)
        val lastCheckedAt = context.dataStore.getAsync(GitHubContributorsLastCheckedAtKey, 0L)
        val now = System.currentTimeMillis()

        val cachedContributors =
            cachedJson
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { parseContributorsJson(it) }.getOrNull() }

        if (!cachedContributors.isNullOrEmpty()) {
            contributorsState = ContributorsState.Loaded(cachedContributors)
        }

        val shouldCheckNetwork =
            cachedJson.isNullOrBlank() || (now - lastCheckedAt) >= ContributorsCacheCheckIntervalMs

        if (!shouldCheckNetwork) {
            if (cachedContributors.isNullOrEmpty()) contributorsState = ContributorsState.Error
            return@LaunchedEffect
        }

        val networkResult =
            runCatching {
                fetchRepoContributorsNetwork(
                    client = httpClient,
                    owner = "koiverse",
                    repo = "ArchiveTune",
                    cachedEtag = cachedEtag,
                )
            }.getOrNull()

        if (networkResult == null) {
            if (cachedContributors.isNullOrEmpty()) contributorsState = ContributorsState.Error
            return@LaunchedEffect
        }

        moe.koiverse.archivetune.utils.PreferenceStore.launchEdit(context.dataStore) {
            this[GitHubContributorsLastCheckedAtKey] = now
            networkResult.etag?.let { this[GitHubContributorsEtagKey] = it }
            networkResult.body?.let { this[GitHubContributorsJsonKey] = it }
        }

        when {
            networkResult.status == HttpStatusCode.NotModified -> {
                if (cachedContributors.isNullOrEmpty()) {
                    contributorsState = ContributorsState.Error
                }
            }

            (networkResult.status.value in 200..299) && !networkResult.body.isNullOrBlank() -> {
                val contributors = runCatching { parseContributorsJson(networkResult.body) }.getOrNull()
                if (!contributors.isNullOrEmpty()) {
                    contributorsState = ContributorsState.Loaded(contributors)
                } else if (cachedContributors.isNullOrEmpty()) {
                    contributorsState = ContributorsState.Error
                }
            }

            else -> {
                if (cachedContributors.isNullOrEmpty()) contributorsState = ContributorsState.Error
            }
        }
    }

    val teamMembers = listOf(
        TeamMember(
            avatarUrl = "https://avatar-api.koiisannn.cloud/discord/avatar/886971572668219392",
            name = "Koiverse",
            position = "always on mode UwU",
            profileUrl = "https://github.com/koiverse",
            github = "https://github.com/koiverse",
            website = "https://koiiverse.cloud", // If blank, hide OutlinedIconChip for website
            discord = "https://discord.com/users/886971572668219392"
        ),
        TeamMember(
            avatarUrl = "https://avatars.githubusercontent.com/u/93458424?v=4",
            name = "WTTexe",
            position = "Word Synced Lyrics, Gradients and UI Changes for the better!",
            profileUrl = "https://github.com/Windowstechtips",
            github = "https://github.com/Windowstechtips",
            website = null,
            discord = "https://discord.com/users/840839409640800258"
        ),
        TeamMember(
            avatarUrl = "https://avatars.githubusercontent.com/u/80542861?v=4",
            name = "MO AGAMY",
            position = "Metrolist Dev",
            profileUrl = "https://github.com/mostafaalagamy",
            github = "https://github.com/mostafaalagamy",
            website = null,
            discord = null
        ),
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
                    onClick = { uriHandler.openUri("https://archivetune.koiiverse.cloud") },
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable(enabled = member.profileUrl != null) {
                                member.profileUrl?.let { uriHandler.openUri(it) }
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
                                model = member.avatarUrl,
                                contentDescription = member.name,
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
                                    text = member.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = member.position,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )

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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Awesome contributor",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        ContributorGrid(
                            state = contributorsState,
                            onOpenProfile = uriHandler::openUri,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContributorGrid(
    state: ContributorsState,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contributors = when (state) {
        ContributorsState.Loading -> null
        ContributorsState.Error -> emptyList()
        is ContributorsState.Loaded -> state.contributors.take(20)
    }

    val columns = 4
    val spacing = 10.dp
    BoxWithConstraints(modifier = modifier) {
        val itemWidth = (maxWidth - spacing * (columns - 1)) / columns
        val tileShape = RoundedCornerShape(22.dp)
        val tileColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

        FlowRow(
            maxItemsInEachRow = columns,
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (contributors == null) {
                repeat(6) {
                    Surface(
                        shape = tileShape,
                        color = tileColor,
                        modifier = Modifier.width(itemWidth)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .height(14.dp)
                                    .fillMaxWidth(0.7f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                        }
                    }
                }
            } else {
                contributors.forEach { contributor ->
                    Surface(
                        shape = tileShape,
                        color = tileColor,
                        modifier = Modifier
                            .width(itemWidth)
                            .clickable(enabled = contributor.profileUrl.isNotBlank()) {
                                if (contributor.profileUrl.isNotBlank()) {
                                    onOpenProfile(contributor.profileUrl)
                                }
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 10.dp)
                        ) {
                            AsyncImage(
                                model = contributor.avatarUrl,
                                contentDescription = contributor.login,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = contributor.login,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
