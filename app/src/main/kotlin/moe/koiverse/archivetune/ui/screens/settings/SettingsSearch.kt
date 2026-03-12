/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.settings

fun filterQuickActions(
    actions: List<SettingsQuickAction>,
    query: String,
): List<SettingsQuickAction> {
    if (query.isBlank()) return actions
    return actions.filter { it.label.contains(query, ignoreCase = true) }
}

fun filterSettingsGroups(
    groups: List<SettingsGroup>,
    query: String,
): List<SettingsGroup> {
    if (query.isBlank()) return groups
    return groups.mapNotNull { group ->
        if (group.title.contains(query, ignoreCase = true)) {
            group
        } else {
            val filtered = group.items.filter { matchesQuery(it, query) }
            if (filtered.isEmpty()) null else group.copy(items = filtered)
        }
    }
}

fun matchesQuery(
    item: SettingsItem,
    query: String,
): Boolean {
    if (item.title.contains(query, ignoreCase = true)) return true
    if (item.subtitle?.contains(query, ignoreCase = true) == true) return true
    if (item.badge?.contains(query, ignoreCase = true) == true) return true
    return item.keywords.any { keyword ->
        keyword.contains(query, ignoreCase = true) ||
            query.contains(keyword, ignoreCase = true)
    }
}

fun filterInternalItems(
    items: List<SettingsItem>,
    query: String,
): List<SettingsItem> {
    if (query.isBlank()) return emptyList()
    return items.filter { matchesQuery(it, query) }
}

fun filterIntegrations(
    integrations: List<SettingsIntegrationAction>,
    query: String,
): List<SettingsIntegrationAction> {
    if (query.isBlank()) return integrations
    return integrations.filter { it.label.contains(query, ignoreCase = true) }
}
