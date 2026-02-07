/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.my.kizzy.gateway.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Resume(
    @SerialName("seq")
    val seq: Int,
    @SerialName("session_id")
    val sessionId: String?,
    @SerialName("token")
    val token: String,
)