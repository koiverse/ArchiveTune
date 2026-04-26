/*
 * ArchiveTune Project Original (2026)
 * Chartreux Westia (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 * Don't remove this copyright holder!
 */

package moe.koiverse.archivetune.paxsenix.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaxsenixStats(
    @SerialName("uptime_seconds") val uptimeSeconds: Double,
    @SerialName("started_at") val startedAt: String,
    @SerialName("total_requests") val totalRequests: Int,
    @SerialName("successful_requests") val successfulRequests: Int,
    @SerialName("failed_requests") val failedRequests: Int,
    @SerialName("overall_success_rate") val overallSuccessRate: String,
    val endpoints: Map<String, EndpointStats> = emptyMap(),
    val providers: Map<String, ProviderStats> = emptyMap(),
    @SerialName("request_log") val requestLog: List<RequestLogEntry> = emptyList()
)

@Serializable
data class EndpointStats(
    val hits: Int,
    val errors: Int,
    @SerialName("success_rate") val successRate: String,
    @SerialName("avg_response_time_ms") val avgResponseTimeMs: Double,
    @SerialName("last_accessed") val lastAccessed: String
)

@Serializable
data class ProviderStats(
    val hits: Int,
    val errors: Int,
    @SerialName("success_rate") val successRate: String
)

@Serializable
data class RequestLogEntry(
    val timestamp: String,
    val endpoint: String,
    val provider: String,
    val success: Boolean,
    @SerialName("response_time_ms") val responseTimeMs: Double,
    val ip: String,
    @SerialName("user_agent") val userAgent: String
)
