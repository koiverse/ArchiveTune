/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.ui.screens.settings

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import moe.koiverse.archivetune.db.entities.Song
import moe.koiverse.archivetune.utils.DiscordRPC
import moe.koiverse.archivetune.utils.DiscordImageResolver
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

object DiscordPresenceManager {
    private val started = AtomicBoolean(false)
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var rpcInstance: DiscordRPC? = null
    private var rpcToken: String? = null
    private val logTag = "DiscordPresenceManager"

    // Stored start parameters so we can restart the updater later.
    // We intentionally store the application Context (or whatever the caller passed) — callers
    // should prefer passing an Application context to avoid leaking Activities.
    private var lastStartContext: Context? = null
    private var lastToken: String? = null
    private var lastSongProvider: (() -> Song?)? = null
    private var lastPositionProvider: (() -> Long)? = null
    private var lastIsPausedProvider: (() -> Boolean)? = null
    private var lastIntervalProvider: (() -> Long)? = null
    private var lastPresenceUpdateTime = 0L
    private const val MIN_PRESENCE_UPDATE_INTERVAL = 20_000L // 20 seconds debounce
    private var consecutiveFailures = 0
    private const val MAX_CONSECUTIVE_FAILURES = 3
    private var lastRestartTime = 0L
    private const val MIN_RESTART_INTERVAL = 30_000L 
    private var lastFailedRestartDueToParams = 0L
    private const val FAILED_RESTART_LOCKOUT = 60_000L


    // Last successful RPC timestamps (nullable). Exposed as StateFlow so Compose can observe changes.
    private val _lastRpcStartTime = MutableStateFlow<Long?>(null)
    val lastRpcStartTimeFlow = _lastRpcStartTime.asStateFlow()
    val lastRpcStartTime: Long? get() = _lastRpcStartTime.value

    private val _lastRpcEndTime = MutableStateFlow<Long?>(null)
    val lastRpcEndTimeFlow = _lastRpcEndTime.asStateFlow()
    val lastRpcEndTime: Long? get() = _lastRpcEndTime.value
    private val rpcMutex = Mutex()

    /** Public helper to update the last RPC timestamps from callers. */
    fun setLastRpcTimestamps(start: Long?, end: Long?) {
        _lastRpcStartTime.value = start
        _lastRpcEndTime.value = end
    }

    suspend fun getOrCreateRpc(context: Context, token: String): DiscordRPC {
        if (rpcInstance == null || rpcToken != token) {
            try {
                rpcInstance?.stopActivity()
            } catch (ex: Exception) {
                Timber.tag(logTag).v(ex, "failed to stopActivity on previous RPC instance")
            }

            try {
                rpcInstance?.closeRPC()
            } catch (ex: Exception) {
                Timber.tag(logTag).v(ex, "failed to close previous RPC instance")
            }

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
        isPaused: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        rpcMutex.withLock {
            try {
                if (token.isBlank()) {
                    Timber.tag(logTag).w("updatePresence skipped (token missing)")
                    return@withLock false
                }

                if (song == null) {
                    val rpc = getOrCreateRpc(context, token)
                    rpc.stopActivity()
                    Timber.tag(logTag).d("cleared presence (no song)")
                    consecutiveFailures = 0
                    return@withLock true
                }

                try {
                    withTimeout(8_000L) {
                        DiscordImageResolver.resolveImagesForSong(context, song)
                    }
                } catch (e: Exception) {
                    Timber.tag(logTag).v(e, "image resolution for presence failed or timed out")
                }

                val rpc = getOrCreateRpc(context, token)
                val result = rpc.updateSong(song, positionMs, isPaused)
                if (result.isSuccess) {
                    consecutiveFailures = 0
                    Timber.tag(logTag).d(
                        "updatePresence success (song=%s, paused=%s)",
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
                    consecutiveFailures++
                    Timber.tag(logTag).w("updatePresence failed silently — updateSong returned failure (consecutive=%d)", consecutiveFailures)
                    false
                }
            } catch (ex: Exception) {
                consecutiveFailures++
                Timber.tag(logTag).e(ex, "updatePresence failed (consecutive=%d)", consecutiveFailures)
                false
            }
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
        lastStartContext = context
        lastToken = token
        lastSongProvider = songProvider
        lastPositionProvider = positionProvider
        lastIsPausedProvider = isPausedProvider
        lastIntervalProvider = intervalProvider

        if (started.getAndSet(true)) return

        resetFailureCount()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        job = scope!!.launch {
            // Perform an immediate first update (or at the first second of the interval).
            try {
                // switch to Main for player access
                val (firstSong, firstPosition, firstIsPaused) = withContext(Dispatchers.Main) {
                    Triple(songProvider(), positionProvider(), isPausedProvider())
                }

                // Try resolving and persisting image URLs before update so DiscordRPC can use saved artwork immediately.
                try {
                    firstSong?.let { song ->
                        DiscordImageResolver.resolveImagesForSong(context, song)
                    }
                } catch (e: Exception) {
                    Timber.tag(logTag).v(e, "initial image resolution failed")
                }

                // Run the first update immediately
                try {
                    val firstResult = updatePresence(
                        context = context,
                        token = token,
                        song = firstSong,
                        positionMs = firstPosition,
                        isPaused = firstIsPaused,
                    )
                    Timber.tag(logTag).d("initial updatePresence result=%s songId=%s", firstResult, firstSong?.song?.id)
                } catch (e: Exception) {
                    Timber.tag(logTag).e(e, "initial updatePresence failed")
                }
            } catch (e: Exception) {
                Timber.tag(logTag).e(e, "initial first-run failed")
            }

            while (isActive) {
                try {
                    // switch to Main for player access
                    val (song, position, isPaused) = withContext(Dispatchers.Main) {
                        Triple(songProvider(), positionProvider(), isPausedProvider())
                    }

                    val success = updatePresence(
                        context = context,
                        token = token,
                        song = song,
                        positionMs = position,
                        isPaused = isPaused,
                    )

                    // optional: handle `success` if needed
                } catch (e: CancellationException) {
                    Timber.tag(logTag).d("updater cancelled")
                    break
                } catch (e: Exception) {
                    Timber.tag(logTag).e(e, "loop error → ${e.message}")
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

    /**
     * Restart the manager using the most recent parameters passed to `start()`.
     * Returns true if restart was scheduled, false if there were no stored parameters or too many recent failures.
     */
    fun restart(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastRestartTime < MIN_RESTART_INTERVAL) {
            Timber.tag(logTag).w("restart skipped (too soon since last restart, wait %dms)", MIN_RESTART_INTERVAL - (now - lastRestartTime))
            return false
        }
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            Timber.tag(logTag).w("restart skipped (too many consecutive failures: %d)", consecutiveFailures)
            return false
        }

        val ctx = lastStartContext
        val token = lastToken
        val songProv = lastSongProvider
        val posProv = lastPositionProvider
        val pausedProv = lastIsPausedProvider
        val intervalProv = lastIntervalProvider

        if (ctx == null || token == null || songProv == null || posProv == null || pausedProv == null || intervalProv == null) {
            if (now - lastFailedRestartDueToParams < FAILED_RESTART_LOCKOUT) {
                Timber.tag(logTag).w("restart skipped (lockout after missing params, wait %dms)", FAILED_RESTART_LOCKOUT - (now - lastFailedRestartDueToParams))
                return false
            }
            lastFailedRestartDueToParams = now
            Timber.tag(logTag).w("restart skipped (missing previous start parameters)")
            return false
        }

        lastRestartTime = now
        lastFailedRestartDueToParams = 0L
        stop()
        start(ctx, token, songProv, posProv, pausedProv, intervalProv)
        Timber.tag(logTag).d("restarted")
        return true
    }
    
    fun resetFailureCount() {
        consecutiveFailures = 0
    }

    /** Run update immediately. */
    suspend fun updateNow(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
    ): Boolean = updatePresence(
        context = context,
        token = token,
        song = song,
        positionMs = positionMs,
        isPaused = isPaused,
    )

    /** Stop the manager. */
    fun stop() {
        if (!started.getAndSet(false)) return
        
        val rpcToClose = rpcInstance
        rpcInstance = null
        rpcToken = null
        
        job?.cancel()
        job = null
        scope?.cancel()
        scope = null
        lifecycleObserver?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
        lifecycleObserver = null

        if (rpcToClose != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    rpcToClose.stopActivity()
                } catch (ex: Exception) {
                    Timber.tag(logTag).v(ex, "stopActivity failed during stop()")
                }
                try {
                    rpcToClose.closeRPC()
                } catch (ex: Exception) {
                    Timber.tag(logTag).v(ex, "closeRPC failed during stop()")
                }
            }
        }

        Timber.tag(logTag).d("stopped")
    }

    fun isRunning(): Boolean = started.get()
}
