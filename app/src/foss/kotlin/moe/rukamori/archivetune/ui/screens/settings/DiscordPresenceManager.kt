package moe.rukamori.archivetune.ui.screens.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import moe.rukamori.archivetune.db.entities.Song

object DiscordPresenceManager {
    private val lastRpcStartTimeState = MutableStateFlow<Long?>(null)
    val lastRpcStartTimeFlow = lastRpcStartTimeState.asStateFlow()
    val lastRpcStartTime: Long? get() = lastRpcStartTimeState.value

    private val lastRpcEndTimeState = MutableStateFlow<Long?>(null)
    val lastRpcEndTimeFlow = lastRpcEndTimeState.asStateFlow()
    val lastRpcEndTime: Long? get() = lastRpcEndTimeState.value

    fun setLastRpcTimestamps(start: Long?, end: Long?) {
        lastRpcStartTimeState.value = start
        lastRpcEndTimeState.value = end
    }

    suspend fun updatePresence(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
    ): Boolean = false

    fun start(
        context: Context,
        token: String,
        songProvider: () -> Song?,
        positionProvider: () -> Long,
        isPausedProvider: () -> Boolean,
        intervalProvider: () -> Long,
    ) = Unit

    fun restart(): Boolean = false

    fun resetFailureCount() = Unit

    suspend fun updateNow(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
    ): Boolean = false

    fun stop() = Unit

    fun isRunning(): Boolean = false
}
