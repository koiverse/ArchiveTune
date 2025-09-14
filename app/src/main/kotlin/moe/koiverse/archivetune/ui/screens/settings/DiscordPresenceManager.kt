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

object DiscordPresenceManager {
    private val started = AtomicBoolean(false)
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var rpcInstance: DiscordRPC? = null
    private var rpcToken: String? = null

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

    fun getOrCreateRpc(context: Context, token: String): DiscordRPC {
        if (rpcInstance == null || rpcToken != token) {
            rpcInstance?.closeRPC()
            rpcInstance = DiscordRPC(context, token)
            rpcToken = token
        }
        return rpcInstance!!
    }

    /**
     * Core updater: update or clear Discord presence.
     */
    suspend fun updatePresence(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (token.isBlank()) {
                Timber.w("DiscordPresenceManager: updatePresence skipped (token missing)")
                return@withContext false
            }

            if (song == null) {
                val rpc = getOrCreateRpc(context, token)
                rpc.stopActivity()
                Timber.d("DiscordPresenceManager: cleared presence (no song)")
                return@withContext true
            }

            val rpc = getOrCreateRpc(context, token)
            val result = rpc.updateSong(song, positionMs, isPaused)
            if (result.isSuccess) {
                Timber.d(
                    "DiscordPresenceManager: updatePresence success (song=%s, paused=%s)",
                    song.song.title,
                    isPaused
                )

                if (!isPaused) {
                    val now = System.currentTimeMillis()
                    val calculatedStartTime = now - positionMs
                    val calculatedEndTime = calculatedStartTime + song.song.duration * 1000L
                    setLastRpcTimestamps(calculatedStartTime, calculatedEndTime)
                }
                true
            } else {
                Timber.w("DiscordPresenceManager: updatePresence failed silently")
                false
            }
        } catch (ex: Exception) {
            Timber.e(ex, "DiscordPresenceManager: updatePresence failed")
            false
        }
    }

    /**
     * Start background updater.
     */
    fun start(
        context: Context,
        token: String,
        songProvider: () -> Song?,
        positionProvider: () -> Long,
        isPausedProvider: () -> Boolean,
        intervalProvider: () -> Long
    ) {
        if (started.getAndSet(true)) return // <-- ensure only one job runs
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        job = scope!!.launch {
            while (isActive) {
                try {
                    val song = songProvider()
                    val position = positionProvider()
                    val isPaused = isPausedProvider()

                    val success = updatePresence(
                        context = context,
                        token = token,
                        song = song,
                        positionMs = position,
                        isPaused = isPaused
                    )
                    Timber.d("DiscordPresenceManager: background update executed, success=$success")
                } catch (e: CancellationException) {
                    Timber.d("DiscordPresenceManager: updater cancelled")
                    break
                } catch (e: Exception) {
                    // log reason clearly
                    Timber.e(e, "DiscordPresenceManager: loop error → ${e.message}")
                }

                val delayMs = intervalProvider()
                if (delayMs <= 0L) break
                delay(delayMs)
            }
        }
        lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                stop()
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver!!)
    }

    /** Run update immediately. */
    suspend fun updateNow(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean
    ): Boolean = updatePresence(
        context = context,
        token = token,
        song = song,
        positionMs = positionMs,
        isPaused = isPaused
    )

    /** Stop the manager. */
    fun stop() {
        if (!started.getAndSet(false)) return
        job?.cancel()
        job = null
        scope?.cancel()
        scope = null
        lifecycleObserver?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
        lifecycleObserver = null

        rpcInstance?.closeRPC()
        rpcInstance = null
        rpcToken = null
        Timber.d("DiscordPresenceManager: stopped")
    }

    fun isRunning(): Boolean = started.get()
}
