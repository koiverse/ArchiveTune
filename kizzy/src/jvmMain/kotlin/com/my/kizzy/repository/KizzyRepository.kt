/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * KizzyRepositoryImpl.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.repository

import com.my.kizzy.remote.ApiService
import com.my.kizzy.utils.toImageAsset

/**
 * Modified by Koiverse
 */
class KizzyRepository {
    private val api = ApiService()

    // Simple in-memory cache for resolved image ids to speed up repeated resolves.
    // Keep it very small to avoid memory pressure.
    private val imageCache = object {
        private val map = LinkedHashMap<String, String>(16, 0.75f, true)
        private val maxSize = 64
        @Synchronized
        fun get(key: String): String? = map[key]
        @Synchronized
        fun put(key: String, value: String) {
            map[key] = value
            if (map.size > maxSize) {
                // remove eldest entry (LinkedHashMap with accessOrder=true maintains LRU ordering)
                val it = map.entries.iterator()
                if (it.hasNext()) {
                    it.next()
                    it.remove()
                }
            }
        }
        @Synchronized
        fun remove(key: String) {
            map.remove(key)
        }
    }

    /**
     * Public helper to seed the in-memory cache from callers (e.g. when an external
     * storage has a previously resolved mapping). This avoids re-resolving the same
     * external URL repeatedly.
     */
    @Synchronized
    fun putToCache(key: String?, value: String?) {
        if (key.isNullOrBlank() || value.isNullOrBlank()) return
        imageCache.put(key, value)
    }

    /** Peek the in-memory cache for a key. */
    @Synchronized
    fun peekCache(key: String?): String? {
        if (key.isNullOrBlank()) return null
        return imageCache.get(key)
    }

    /** Remove a mapping from in-memory cache. */
    @Synchronized
    fun removeCache(key: String?) {
        if (key.isNullOrBlank()) return
        imageCache.remove(key)
    }

    suspend fun getImage(url: String): String? {
        imageCache.get(url)?.let { return it }
        val result = api.getImage(url).getOrNull()?.toImageAsset()
        if (result != null) imageCache.put(url, result)
        return result
    }

    // Prefetch image in background; returns resolved id or null
    suspend fun prefetchImage(url: String): String? {
        return getImage(url)
    }
}
