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
                val it = map.entries.iterator()
                if (it.hasNext()) it.remove()
            }
        }
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
