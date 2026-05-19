/*
 * ArchiveTune (2026)
 * Â© Chartreux Westia â€” github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.ai

import androidx.compose.runtime.Immutable
import moe.koiverse.archivetune.constants.AiProvider
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.absoluteValue

@Immutable
data class AiServiceConfig(
    val provider: AiProvider,
    val apiKey: String,
    val customEndpoint: String,
) {
    val canCallApi: Boolean
        get() = provider != AiProvider.NONE &&
            apiKey.isNotBlank() &&
            (provider != AiProvider.CUSTOM || customEndpoint.isNotBlank())
}

@Immutable
data class AiUserMix(
    val id: String,
    val title: String,
    val query: String,
    val stripeColor: Long,
)

object AiUserMixJson {
    fun encode(items: List<AiUserMix>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("query", item.query)
                    .put("stripeColor", item.stripeColor.toString()),
            )
        }
        return array.toString()
    }

    fun decode(raw: String): List<AiUserMix> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val title = item.optString("title").trim()
                    val query = item.optString("query").trim()
                    if (title.isBlank() || query.isBlank()) continue
                    add(
                        AiUserMix(
                            id = item.optString("id").takeIf { it.isNotBlank() }
                                ?: stableMixId(title = title, query = query, index = index),
                            title = title,
                            query = query,
                            stripeColor = item.optString("stripeColor")
                                .toLongOrNull()
                                ?: mixStripeColor(title = title, query = query, index = index),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun fromAiResult(
        title: String,
        query: String,
        index: Int,
    ): AiUserMix =
        AiUserMix(
            id = stableMixId(title = title, query = query, index = index),
            title = title.trim(),
            query = query.trim(),
            stripeColor = mixStripeColor(title = title, query = query, index = index),
        )

    private fun stableMixId(
        title: String,
        query: String,
        index: Int,
    ): String = "ai_mix_${(title + "|" + query + "|" + index).hashCode().absoluteValue}"

    private fun mixStripeColor(
        title: String,
        query: String,
        index: Int,
    ): Long {
        val colors = longArrayOf(
            0xFF6750A4,
            0xFF006A6A,
            0xFFB3261E,
            0xFF5D6B00,
            0xFF7D5260,
            0xFF006D3B,
            0xFF984061,
            0xFF4A5C92,
            0xFF855400,
            0xFF006C4C,
        )
        val hash = (title + query + index).hashCode().absoluteValue
        return colors[hash % colors.size]
    }
}
