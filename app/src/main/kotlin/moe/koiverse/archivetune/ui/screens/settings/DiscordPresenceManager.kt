package moe.koiverse.archivetune.ui.screens.settings

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
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
    // Last successful RPC timestamps (nullable). Updated by manager and manual refresh flows.
    @Volatile
    var lastRpcStartTime: Long? = null
        private set

    @Volatile
    var lastRpcEndTime: Long? = null
        private set

    /** Public helper to update the last RPC timestamps from callers. */
    fun setLastRpcTimestamps(start: Long?, end: Long?) {
        lastRpcStartTime = start
        lastRpcEndTime = end
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
                    val res = try { update(); true } catch (_: Exception) { false }
                    Timber.d("DiscordPresenceManager: background update executed, success=%s", res)
                } catch (_: Exception) {}
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
                update()
            } catch (_: Exception) {
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
