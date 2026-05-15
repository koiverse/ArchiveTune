/*
 * ArchiveTune (2026)
 * © Chartreux Westia — github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */





package com.my.kizzy.gateway.entities

import com.my.kizzy.gateway.entities.op.OpCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Payload(
    @SerialName("t")
    val t: String? = null,
    @SerialName("s")
    val s: Int? = null,
    @SerialName("op")
    val op: OpCode? = null,
    @SerialName("d")
    val d: JsonElement? = null,
)