/*
 * ArchiveTune (2026)
 * Â© Chartreux Westia â€” github.com/koiverse
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.koiverse.archivetune.ai

import org.json.JSONArray
import org.json.JSONObject

internal fun extractJsonArray(raw: String): JSONArray {
    val trimmed = raw.trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    val start = trimmed.indexOf('[')
    val end = trimmed.lastIndexOf(']')
    require(start >= 0 && end > start) { "AI response did not contain a JSON array" }
    return JSONArray(trimmed.substring(start, end + 1))
}

internal fun JSONObject.readErrorMessage(): String? {
    val error = optJSONObject("error")
    return error?.optString("message")?.takeIf { it.isNotBlank() }
        ?: optString("error").takeIf { it.isNotBlank() }
}
