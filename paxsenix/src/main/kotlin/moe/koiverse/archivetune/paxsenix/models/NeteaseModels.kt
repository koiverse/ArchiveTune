/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.paxsenix.models

import kotlinx.serialization.Serializable

@Serializable
data class NeteaseSearchResponse(
    val result: NeteaseSearchResult? = null
)

@Serializable
data class NeteaseSearchResult(
    val songs: List<NeteaseSong> = emptyList()
)

@Serializable
data class NeteaseSong(
    val id: Long = 0,
    val name: String? = null,
    val artists: List<NeteaseArtist> = emptyList(),
    val duration: Int = 0
)

@Serializable
data class NeteaseArtist(
    val name: String
)

@Serializable
data class NeteaseLyricsResponse(
    val lrc: NeteaseLrc? = null
)

@Serializable
data class NeteaseLrc(
    val lyric: String? = null
)
