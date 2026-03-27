/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.player

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import moe.koiverse.archivetune.utils.YTPlayerUtils

internal enum class PlaybackErrorKind {
    ConfirmationRequired,
    NoInternet,
    Timeout,
    NoStream,
    Decoder,
    Http,
    Unknown,
}

internal data class PlaybackErrorInfo(
    val kind: PlaybackErrorKind,
    val httpCode: Int?,
    val loginRecoveryUrl: String?,
)

internal fun PlaybackException.toPlaybackErrorInfo(currentMediaId: String? = null): PlaybackErrorInfo {
    val httpCode = httpStatusCodeOrNull()
    val loginRecoveryUrl = loginRecoveryUrl(currentMediaId)
    val kind =
        when {
            loginRecoveryUrl != null -> PlaybackErrorKind.ConfirmationRequired
            errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> PlaybackErrorKind.NoInternet
            errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> PlaybackErrorKind.Timeout
            httpCode in setOf(403, 404, 410, 416) -> PlaybackErrorKind.NoStream
            errorCode in setOf(
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            ) -> PlaybackErrorKind.Decoder
            httpCode != null -> PlaybackErrorKind.Http
            else -> PlaybackErrorKind.Unknown
        }

    return PlaybackErrorInfo(
        kind = kind,
        httpCode = httpCode,
        loginRecoveryUrl = loginRecoveryUrl,
    )
}

internal fun PlaybackException.httpStatusCodeOrNull(): Int? {
    var throwable: Throwable? = cause
    while (throwable != null) {
        if (throwable is HttpDataSource.InvalidResponseCodeException) return throwable.responseCode
        throwable = throwable.cause
    }
    return null
}

internal fun PlaybackException.loginRecoveryUrl(currentMediaId: String? = null): String? {
    findCause<YTPlayerUtils.LoginRequiredForPlaybackException>()?.let { return it.targetUrl }

    if (YTPlayerUtils.isBotDetectionException(this)) {
        val mediaId = currentMediaId?.trim().orEmpty()
        return if (mediaId.isNotEmpty()) {
            "https://music.youtube.com/watch?v=$mediaId"
        } else {
            "https://music.youtube.com"
        }
    }

    return null
}

private inline fun <reified T : Throwable> Throwable.findCause(): T? {
    var throwable: Throwable? = this
    while (throwable != null) {
        if (throwable is T) return throwable
        throwable = throwable.cause
    }
    return null
}
