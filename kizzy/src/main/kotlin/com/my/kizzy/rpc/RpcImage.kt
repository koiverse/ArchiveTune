/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * RpcImage.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.rpc

import com.my.kizzy.repository.KizzyRepository

/**
 * Modified by Koiverse
 */
sealed class RpcImage {
    abstract suspend fun resolveImage(repository: KizzyRepository): String?

    class DiscordImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String {
            return if (image.startsWith("mp:")) image else "mp:$image"
        }
    }

    class ExternalImage(val image: String) : RpcImage() {
        override suspend fun resolveImage(repository: KizzyRepository): String? {
            if (image.startsWith("mp:") || image.startsWith("external/") || image.startsWith("attachments/")) {
                return if (image.startsWith("mp:")) image else "mp:$image"
            }
            return repository.getImage(image)
        }
    }
}
