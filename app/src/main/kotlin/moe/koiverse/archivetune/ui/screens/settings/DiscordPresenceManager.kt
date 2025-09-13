package moe.koiverse.archivetune.ui.screens.settings

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

/**
 * A global presence manager that runs outside of any single composable. It is resilient across
 * configuration changes because it is tied to the application process lifecycle and runs on a
 * process-wide CoroutineScope.
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
        // emit into flows (thread-safe)
        _lastRpcStartTime.value = start
        _lastRpcEndTime.value = end
    }


    /**
     * Start the manager if not already started. The update callback is invoked on Dispatchers.IO.
     * intervalProvider returns milliseconds to wait between updates. If it returns <=0, the loop
     * stops.
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


        // Keep it running while the process is alive; observe lifecycle to stop if the process is destroyed
        lifecycleObserver = LifecycleEventObserver { source, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                stop()
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver!!)
    }

    /** Run update immediately. */
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
