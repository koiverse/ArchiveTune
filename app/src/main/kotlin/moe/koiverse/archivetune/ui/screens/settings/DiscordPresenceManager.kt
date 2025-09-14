package moe.koiverse.archivetune.ui.screens.settings

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.utils.DiscordRPC
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A global presence manager that runs outside of any single composable.
 * It is resilient across configuration changes because it is tied to the
 * application process lifecycle and runs on a process-wide CoroutineScope.
 */
object DiscordPresenceManager {
    private val started = AtomicBoolean(false)
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var lifecycleObserver: LifecycleEventObserver? = null

    // Last successful RPC timestamps (nullable). Exposed as StateFlow so Compose can observe changes.
    private val _lastRpcStartTime = MutableStateFlow<Long?>(null)
    val lastRpcStartTimeFlow = _lastRpcStartTime.asStateFlow()
    val lastRpcStartTime: Long? get() = _lastRpcStartTime.value

    private val _lastRpcEndTime = MutableStateFlow<Long?>(null)
    val lastRpcEndTimeFlow = _lastRpcEndTime.asStateFlow()
    val lastRpcEndTime: Long? get() = _lastRpcEndTime.value

    /** Public helper to update the last RPC timestamps from callers. */
    fun setLastRpcTimestamps(start: Long?, end: Long?) {
        _lastRpcStartTime.value = start
        _lastRpcEndTime.value = end
    }

    /**
     * Convenience: immediately update Discord presence with a song.
     */
    suspend fun updateSongNow(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (token.isNotBlank() && song != null) {
                    val rpc = DiscordRPC(context, token)
                    rpc.updateSong(song, positionMs, isPaused).getOrThrow()
                    Timber.d("DiscordPresenceManager: updateSongNow success (song=%s, paused=%s)", song.song.title, isPaused)
                    true
                } else {
                    Timber.w("DiscordPresenceManager: updateSongNow skipped (token or song missing)")
                    false
                }
            } catch (ex: Exception) {
                Timber.e(ex, "DiscordPresenceManager: updateSongNow failed")
                false
            }
        }
    }

    /**
     * Start the manager if not already started.
     * The update callback is invoked on Dispatchers.IO.
     */
    fun start(
        update: suspend () -> Unit,
        intervalProvider: () -> Long
    ) {
        if (started.getAndSet(true)) return

        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        job = scope!!.launch {
            while (isActive) {
                try {
                    val res = try {
                        update()
                        Timber.d("DiscordPresenceManager: update succeeded")
                        true
                    } catch (ex: Exception) {
                        Timber.e(ex, "DiscordPresenceManager: update failed")
                        false
                    }
                    Timber.d("DiscordPresenceManager: background update executed, success=%s", res)
                } catch (ex: Exception) {
                    Timber.e(ex, "DiscordPresenceManager: loop error %s", ex.message)
                }
                val delayMs = intervalProvider()
                if (delayMs <= 0L) break
                delay(delayMs)
            }
        }

        lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) stop()
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver!!)
    }

    /** Run update immediately and return true on success, false on failure. */
    suspend fun updateNow(update: suspend () -> Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = update()
                Timber.d("DiscordPresenceManager: updateNow succeeded=%s", result)
                result
            } catch (ex: Exception) {
                Timber.e(ex, "DiscordPresenceManager: updateNow failed")
                false
            }
        }
    }

    /** Stop the manager. */
    fun stop() {
        if (!started.getAndSet(false)) return
        job?.cancel()
        job = null
        scope?.cancel()
        scope = null
        lifecycleObserver?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
        lifecycleObserver = null
    }

    fun isRunning(): Boolean = started.get()
}
