package moe.koiverse.archivetune.extensions.system.ui

object UiSlots {
    const val PLAYER_OVERLAY = "player_overlay"
    const val LYRICS_OVERLAY = "lyrics_overlay"
    const val QUEUE_OVERLAY = "queue_overlay"
    const val SEARCH_FILTER = "search_filter"
    const val SETTINGS_ENTRY = "settings_entry"

    fun slot(id: String): String = "slot_" + id
    fun topBarActions(route: String): String = "topbar_actions_" + route
    fun bottomBar(route: String): String = "bottombar_" + route
    fun fab(route: String): String = "fab_" + route
    fun contextMenu(contextId: String, itemType: String): String = "context_" + contextId + "_" + itemType
    fun navItem(position: String): String = "nav_item_" + position
    fun homeWidget(id: String): String = "home_widget_" + id
}
