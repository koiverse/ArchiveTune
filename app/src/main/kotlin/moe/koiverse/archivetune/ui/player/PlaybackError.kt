/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import moe.koiverse.archivetune.R

@Composable
fun PlaybackError(
    error: PlaybackException,
    retry: () -> Unit,
) {
    val fallbackUnknown = stringResource(R.string.error_unknown)
    val fallbackNoInternet = stringResource(R.string.error_no_internet)
    val fallbackTimeout = stringResource(R.string.error_timeout)
    val fallbackNoStream = stringResource(R.string.error_no_stream)
    val httpCode = error.httpStatusCodeOrNull()
    val message =
        when {
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> fallbackNoInternet
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> fallbackTimeout
            httpCode in setOf(403, 404, 410, 416) -> fallbackNoStream
            httpCode != null -> "$fallbackUnknown (HTTP $httpCode)"
            else -> error.cause?.message?.takeIf { it.isNotBlank() }
                ?: error.message?.takeIf { it.isNotBlank() }
                ?: fallbackUnknown
        }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = retry),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = stringResource(R.string.retry),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.82f),
                    maxLines = 1,
                )
            }

            OutlinedButton(
                onClick = retry,
            ) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

private fun PlaybackException.httpStatusCodeOrNull(): Int? {
    var t: Throwable? = cause
    while (t != null) {
        if (t is HttpDataSource.InvalidResponseCodeException) return t.responseCode
        t = t.cause
    }
    return null
}
