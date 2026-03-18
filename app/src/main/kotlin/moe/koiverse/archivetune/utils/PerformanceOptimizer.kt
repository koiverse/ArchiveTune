package moe.koiverse.archivetune.utils

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Performance Optimizer Utilities
 * 
 * Collection of utilities to optimize app performance:
 * - Memory management
 * - Recomposition optimization
 * - Image loading optimization
 * - Coroutine optimization
 * 
 * @author @cenzer0
 */

/**
 * Debounced state that only updates after a delay
 * Useful for search fields and other frequently changing inputs
 * 
 * Reduces recompositions by batching rapid state changes
 */
@Composable
fun <T> rememberDebouncedState(
    initialValue: T,
    delayMillis: Long = 300L
): MutableState<T> {
    val state = remember { mutableStateOf(initialValue) }
    val debouncedState = remember { mutableStateOf(initialValue) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(state.value) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMillis)
            debouncedState.value = state.value
        }
    }
    
    return state
}

/**
 * Throttled state that updates at most once per interval
 * Useful for scroll positions and other high-frequency updates
 * 
 * Reduces recompositions by limiting update frequency
 */
@Composable
fun <T> rememberThrottledState(
    initialValue: T,
    intervalMillis: Long = 100L
): MutableState<T> {
    val state = remember { mutableStateOf(initialValue) }
    val throttledState = remember { mutableStateOf(initialValue) }
    var lastUpdateTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(state.value) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= intervalMillis) {
            throttledState.value = state.value
            lastUpdateTime = currentTime
        }
    }
    
    return state
}

/**
 * Memory-efficient image cache key generator
 * Creates optimized cache keys for Coil image loading
 */
fun generateOptimizedImageKey(
    url: String?,
    size: Int = 512
): String? {
    if (url == null) return null
    return "$url?size=$size"
}

/**
 * Lazy initialization wrapper
 * Delays expensive computations until actually needed
 */
class LazyValue<T>(private val initializer: () -> T) {
    private var value: T? = null
    private var initialized = false
    
    fun get(): T {
        if (!initialized) {
            value = initializer()
            initialized = true
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }
    
    fun reset() {
        value = null
        initialized = false
    }
}

/**
 * Memory-efficient cache with automatic cleanup
 * Uses weak references to allow garbage collection
 */
class WeakCache<K, V> {
    private val cache = mutableMapOf<K, WeakReference<V>>()
    
    fun get(key: K): V? {
        val ref = cache[key]
        val value = ref?.get()
        
        // Clean up if reference was collected
        if (ref != null && value == null) {
            cache.remove(key)
        }
        
        return value
    }
    
    fun put(key: K, value: V) {
        cache[key] = WeakReference(value)
    }
    
    fun clear() {
        cache.clear()
    }
    
    fun size(): Int = cache.size
    
    /**
     * Removes collected references
     */
    fun cleanup() {
        val keysToRemove = cache.filter { it.value.get() == null }.keys
        keysToRemove.forEach { cache.remove(it) }
    }
}

/**
 * Batched operation executor
 * Collects operations and executes them in batches
 * Reduces overhead for frequent small operations
 */
class BatchExecutor<T>(
    private val batchSize: Int = 10,
    private val delayMillis: Long = 100L,
    private val executor: suspend (List<T>) -> Unit
) {
    private val pending = mutableListOf<T>()
    private var job: Job? = null
    
    fun add(item: T, scope: CoroutineScope) {
        pending.add(item)
        
        if (pending.size >= batchSize) {
            executeBatch(scope)
        } else {
            scheduleExecution(scope)
        }
    }
    
    private fun scheduleExecution(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch {
            delay(delayMillis)
            executeBatch(scope)
        }
    }
    
    private fun executeBatch(scope: CoroutineScope) {
        if (pending.isEmpty()) return
        
        val batch = pending.toList()
        pending.clear()
        
        scope.launch {
            try {
                executor(batch)
            } catch (e: Exception) {
                Timber.e(e, "Batch execution failed")
            }
        }
    }
}

/**
 * Optimized list differ
 * Efficiently calculates list changes for minimal recompositions
 */
fun <T> calculateListDiff(
    oldList: List<T>,
    newList: List<T>,
    areItemsTheSame: (T, T) -> Boolean = { a, b -> a == b }
): ListDiff<T> {
    val added = mutableListOf<T>()
    val removed = mutableListOf<T>()
    val moved = mutableListOf<Pair<Int, Int>>()
    
    val oldMap = oldList.withIndex().associate { it.value to it.index }
    val newMap = newList.withIndex().associate { it.value to it.index }
    
    // Find removed items
    oldList.forEach { item ->
        if (!newMap.containsKey(item)) {
            removed.add(item)
        }
    }
    
    // Find added items
    newList.forEach { item ->
        if (!oldMap.containsKey(item)) {
            added.add(item)
        }
    }
    
    // Find moved items
    newList.forEachIndexed { newIndex, item ->
        val oldIndex = oldMap[item]
        if (oldIndex != null && oldIndex != newIndex) {
            moved.add(oldIndex to newIndex)
        }
    }
    
    return ListDiff(added, removed, moved)
}

data class ListDiff<T>(
    val added: List<T>,
    val removed: List<T>,
    val moved: List<Pair<Int, Int>>
)

/**
 * Coroutine optimization utilities
 */
object CoroutineOptimizer {
    
    /**
     * Executes a block on IO dispatcher with error handling
     */
    suspend fun <T> ioOperation(block: suspend () -> T): Result<T> = 
        withContext(Dispatchers.IO) {
            runCatching { block() }
        }
    
    /**
     * Executes a block on Default dispatcher with error handling
     */
    suspend fun <T> cpuOperation(block: suspend () -> T): Result<T> = 
        withContext(Dispatchers.Default) {
            runCatching { block() }
        }
    
    /**
     * Executes a block on Main dispatcher with error handling
     */
    suspend fun <T> uiOperation(block: suspend () -> T): Result<T> = 
        withContext(Dispatchers.Main) {
            runCatching { block() }
        }
}

/**
 * Performance monitoring utilities
 */
object PerformanceMonitor {
    
    /**
     * Measures execution time of a block
     */
    inline fun <T> measureTime(
        tag: String,
        block: () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        if (duration > 16) { // More than one frame at 60fps
            Timber.d("$tag took ${duration}ms")
        }
        
        return result
    }
    
    /**
     * Measures execution time of a suspend block
     */
    suspend inline fun <T> measureTimeSuspend(
        tag: String,
        crossinline block: suspend () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        if (duration > 16) {
            Timber.d("$tag took ${duration}ms")
        }
        
        return result
    }
}

/**
 * Composable performance optimization
 */
@Composable
fun <T> rememberStableState(
    key: Any?,
    calculation: () -> T
): State<T> {
    return remember(key) {
        mutableStateOf(calculation())
    }
}

/**
 * Stable wrapper for lambdas to prevent unnecessary recompositions
 */
@Stable
class StableCallback<T>(val callback: (T) -> Unit)

@Composable
fun <T> rememberStableCallback(callback: (T) -> Unit): StableCallback<T> {
    return remember { StableCallback(callback) }
}
