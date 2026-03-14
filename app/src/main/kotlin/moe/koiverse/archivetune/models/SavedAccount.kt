/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.models

import kotlinx.serialization.Serializable

@Serializable
data class SavedAccount(
    val id: String,
    val cookie: String,
    val poToken: String = "",
    val visitorData: String = "",
    val dataSyncId: String = "",
    val name: String,
    val email: String = "",
    val channelHandle: String = "",
    val avatarUrl: String? = null,
)
