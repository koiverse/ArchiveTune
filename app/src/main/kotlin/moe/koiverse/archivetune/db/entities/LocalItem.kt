/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */




package moe.koiverse.archivetune.db.entities

sealed class LocalItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnailUrl: String?
}
